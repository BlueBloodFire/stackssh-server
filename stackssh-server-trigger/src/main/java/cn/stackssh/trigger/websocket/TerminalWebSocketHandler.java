package cn.stackssh.trigger.websocket;

import cn.stackssh.domain.auth.adapter.port.ITokenVerifierPort;
import cn.stackssh.domain.ssh.service.ISshTerminalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;

/**
 * WebSocket 终端处理器
 * 客户端连接 /ws/terminal?sessionId=xxx&token=xxx 后：
 * - 服务端每 50ms 读取 SSH 输出并推送给客户端
 * - 客户端发来的消息直接写入 SSH 输入流
 */
@Slf4j
@Component
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    @Resource
    private ISshTerminalService sshTerminalService;

    @Resource
    private ITokenVerifierPort tokenVerifierPort;

    /** WebSocket sessionId -> 推送任务 */
    private final Map<String, ScheduledFuture<?>> pushTasks = new ConcurrentHashMap<>();

    /** WebSocket sessionId -> SSH sessionId */
    private final Map<String, String> sessionMapping = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(20, r -> {
                Thread t = new Thread(r, "ws-terminal-push");
                t.setDaemon(true);
                return t;
            });

    @Override
    public void afterConnectionEstablished(WebSocketSession wsSession) {
        String sshSessionId = extractParam(wsSession.getUri(), "sessionId");
        String token = extractParam(wsSession.getUri(), "token");

        if (sshSessionId == null || sshSessionId.isEmpty()) {
            closeQuietly(wsSession, CloseStatus.BAD_DATA.withReason("Missing sessionId"));
            return;
        }
        if (!authenticate(token)) {
            closeQuietly(wsSession, CloseStatus.POLICY_VIOLATION.withReason("Unauthorized"));
            return;
        }

        sessionMapping.put(wsSession.getId(), sshSessionId);
        log.info("WebSocket 终端连接建立 wsId={} sshSessionId={}", wsSession.getId(), sshSessionId);

        // 每 50ms 读取 SSH 输出并推送
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            if (!wsSession.isOpen()) {
                cancelTask(wsSession.getId());
                return;
            }
            try {
                String output = sshTerminalService.readTerminal(sshSessionId);
                if (output != null && !output.isEmpty()) {
                    sendSafely(wsSession, output);
                }
            } catch (Exception e) {
                log.warn("读取终端输出失败 sshSessionId={}: {}", sshSessionId, e.getMessage());
                cancelTask(wsSession.getId());
                closeQuietly(wsSession, CloseStatus.SERVER_ERROR);
            }
        }, 0, 50, TimeUnit.MILLISECONDS);

        pushTasks.put(wsSession.getId(), task);
    }

    @Override
    protected void handleTextMessage(WebSocketSession wsSession, TextMessage message) {
        String sshSessionId = sessionMapping.get(wsSession.getId());
        if (sshSessionId == null) return;
        try {
            sshTerminalService.writeTerminal(sshSessionId, message.getPayload());
        } catch (Exception e) {
            log.warn("写入终端失败 sshSessionId={}: {}", sshSessionId, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) {
        String wsId = wsSession.getId();
        cancelTask(wsId);
        sessionMapping.remove(wsId);
        log.info("WebSocket 终端连接关闭 wsId={} status={}", wsId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession wsSession, Throwable exception) {
        log.warn("WebSocket 传输异常 wsId={}: {}", wsSession.getId(), exception.getMessage());
        cancelTask(wsSession.getId());
        sessionMapping.remove(wsSession.getId());
    }

    // ---- 工具方法 ----

    private boolean authenticate(String token) {
        if (token == null || token.isEmpty()) return false;
        return tokenVerifierPort.isValid(token);
    }

    private synchronized void sendSafely(WebSocketSession session, String data) {
        if (!session.isOpen()) return;
        try {
            session.sendMessage(new TextMessage(data));
        } catch (Exception e) {
            log.debug("发送 WebSocket 消息失败: {}", e.getMessage());
        }
    }

    private void cancelTask(String wsId) {
        ScheduledFuture<?> task = pushTasks.remove(wsId);
        if (task != null) task.cancel(false);
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) session.close(status);
        } catch (Exception ignored) {}
    }

    private String extractParam(URI uri, String key) {
        if (uri == null || uri.getQuery() == null) return null;
        return Arrays.stream(uri.getQuery().split("&"))
                .filter(p -> p.startsWith(key + "="))
                .map(p -> p.substring(key.length() + 1))
                .findFirst()
                .orElse(null);
    }
}
