package cn.stackssh.trigger.websocket;

import cn.stackssh.domain.ssh.service.ISshTerminalService;
import cn.stackssh.trigger.support.EphemeralAccessTicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    @Resource
    private ISshTerminalService sshTerminalService;

    @Resource
    private EphemeralAccessTicketService accessTicketService;

    private final Map<String, ScheduledFuture<?>> pushTasks = new ConcurrentHashMap<>();
    private final Map<String, String> sessionMapping = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(20, runnable -> {
                Thread thread = new Thread(runnable, "ws-terminal-push");
                thread.setDaemon(true);
                return thread;
            });

    @Override
    public void afterConnectionEstablished(WebSocketSession wsSession) {
        String sshSessionId = extractParam(wsSession.getUri(), "sessionId");
        String ticket = extractParam(wsSession.getUri(), "ticket");
        if (sshSessionId == null || sshSessionId.isEmpty()) {
            closeQuietly(wsSession, CloseStatus.BAD_DATA.withReason("Missing sessionId"));
            return;
        }

        try {
            EphemeralAccessTicketService.TicketRecord record =
                    accessTicketService.consumeWebSocketTicket(ticket, sshSessionId);
            if (!sshTerminalService.isSessionOwner(sshSessionId, record.getUserId())) {
                throw new IllegalArgumentException("终端会话访问校验失败");
            }
        } catch (Exception e) {
            closeQuietly(wsSession, CloseStatus.POLICY_VIOLATION.withReason("Unauthorized"));
            return;
        }

        sessionMapping.put(wsSession.getId(), sshSessionId);
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> pushTerminalOutput(wsSession, sshSessionId),
                0, 50, TimeUnit.MILLISECONDS);
        pushTasks.put(wsSession.getId(), task);
    }

    @Override
    protected void handleTextMessage(WebSocketSession wsSession, TextMessage message) {
        String sshSessionId = sessionMapping.get(wsSession.getId());
        if (sshSessionId == null) {
            return;
        }
        try {
            sshTerminalService.writeTerminal(sshSessionId, message.getPayload());
        } catch (IllegalArgumentException e) {
            sendSafely(wsSession, "\r\n[server] " + e.getMessage() + "\r\n");
        } catch (Exception e) {
            log.warn("写入终端失败 sshSessionId={}: {}", sshSessionId, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) {
        cancelTask(wsSession.getId());
        sessionMapping.remove(wsSession.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession wsSession, Throwable exception) {
        cancelTask(wsSession.getId());
        sessionMapping.remove(wsSession.getId());
    }

    private void pushTerminalOutput(WebSocketSession wsSession, String sshSessionId) {
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
            cancelTask(wsSession.getId());
            closeQuietly(wsSession, CloseStatus.SERVER_ERROR);
        }
    }

    private synchronized void sendSafely(WebSocketSession session, String data) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(data));
        } catch (Exception e) {
            log.debug("WebSocket 推送失败: {}", e.getMessage());
        }
    }

    private void cancelTask(String wsId) {
        ScheduledFuture<?> task = pushTasks.remove(wsId);
        if (task != null) {
            task.cancel(false);
        }
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (Exception ignored) {
        }
    }

    private String extractParam(URI uri, String key) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        return Arrays.stream(uri.getQuery().split("&"))
                .filter(pair -> pair.startsWith(key + "="))
                .map(pair -> pair.substring(key.length() + 1))
                .findFirst()
                .orElse(null);
    }
}
