package cn.bugstack.ai.domain.ssh.service.terminal;

import cn.bugstack.ai.domain.ssh.adapter.port.ISshSessionPort;
import cn.bugstack.ai.domain.ssh.adapter.port.ITerminalSessionPort;
import cn.bugstack.ai.domain.ssh.model.entity.TerminalSessionEntity;
import cn.bugstack.ai.domain.ssh.service.ISshTerminalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSH终端领域服务实现
 * 遵循单一职责原则，将终端会话管理委托给基础设施层
 *
 * @author waissh dev
 */
@Slf4j
@Service
public class SshTerminalService implements ISshTerminalService {

    private final ISshSessionPort sshSessionService;
    private final ITerminalSessionPort terminalSessionService;

    /** 会话ID -> 终端会话实体 映射 */
    private final Map<String, TerminalSessionEntity> sessionCache = new ConcurrentHashMap<>();

    public SshTerminalService(ISshSessionPort sshSessionService,
                              ITerminalSessionPort terminalSessionService) {
        this.sshSessionService = sshSessionService;
        this.terminalSessionService = terminalSessionService;
    }

    @Override
    public TerminalSessionEntity openTerminal(String connectionId, int cols, int rows) {
        log.info("打开终端会话 connectionId={} cols={} rows={}", connectionId, cols, rows);

        // 1. 检查SSH连接是否已建立
        if (!sshSessionService.isConnected(connectionId)) {
            throw new IllegalStateException("SSH连接未建立，请先连接");
        }

        // 2. 清理同一 connectionId 的旧会话（基础设施层已关闭 channel，这里清理域层缓存）
        sessionCache.entrySet().removeIf(entry -> {
            if (connectionId.equals(entry.getValue().getConnectionId())) {
                log.info("清理旧终端会话缓存 sessionId={} connectionId={}", entry.getKey(), connectionId);
                return true;
            }
            return false;
        });

        // 3. 通过基础设施层打开终端会话
        String sessionId = terminalSessionService.openTerminal(connectionId, cols, rows);

        // 3. 创建并缓存会话实体
        TerminalSessionEntity entity = TerminalSessionEntity.builder()
                .sessionId(sessionId)
                .connectionId(connectionId)
                .cols(cols)
                .rows(rows)
                .status(1)
                .createdAt(LocalDateTime.now())
                .lastActiveAt(LocalDateTime.now())
                .build();

        sessionCache.put(sessionId, entity);
        log.info("终端会话创建成功 sessionId={}", sessionId);

        return entity;
    }

    /** Agent 执行命令后等待输出的最大时间（ms） */
    private static final long COMMAND_EXEC_WAIT_MS = 2000;

    /** Agent 执行命令后等待输出的检查间隔（ms） */
    private static final long COMMAND_EXEC_CHECK_INTERVAL_MS = 50;

    @Override
    public String executeCommand(String sessionId, String command) {
        log.info("Agent执行命令 sessionId={} command={}", sessionId, command);

        // 1. 校验会话
        TerminalSessionEntity entity = sessionCache.get(sessionId);
        if (entity == null || !entity.isActive()) {
            throw new IllegalArgumentException("终端会话不存在或已关闭");
        }

        // 2. 先清空缓冲区中残留的旧输出（如 prompt 等）
        terminalSessionService.read(sessionId);

        // 3. 回显命令到终端：模拟用户输入效果（\r\n + 命令 + \n 触发执行）
        // 这样在终端面板上能看到独立的一行命令
        String echoLine = "\r\n" + command + "\n";
        terminalSessionService.write(sessionId, echoLine);

        // 4. 更新活跃时间
        entity.touch();

        // 5. 等待命令执行完成：轮询等待输出到达或超时
        // shell 命令执行需要时间，不能立即 read
        long deadline = System.currentTimeMillis() + COMMAND_EXEC_WAIT_MS;
        StringBuilder resultOutput = new StringBuilder();
        
        try {
            // 先短暂等待 shell 处理命令
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 循环读取，直到有输出或超时（检测到 prompt 返回说明命令执行完了）
        while (System.currentTimeMillis() < deadline) {
            String chunk = terminalSessionService.read(sessionId);
            if (chunk != null && !chunk.isEmpty()) {
                resultOutput.append(chunk);
            }
            // 如果输出中包含了 prompt 特征（如 $ 或 # 结尾），说明命令已执行完
            String current = resultOutput.toString();
            if (current.contains("$") || current.contains("#")) {
                // 再等一小段时间确保输出完整
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // 最后再读一次残留
                String finalChunk = terminalSessionService.read(sessionId);
                if (finalChunk != null && !finalChunk.isEmpty()) {
                    resultOutput.append(finalChunk);
                }
                break;
            }
            try {
                Thread.sleep(COMMAND_EXEC_CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 如果还没读到输出，最后再尝试读一次
        if (resultOutput.isEmpty()) {
            String finalChunk = terminalSessionService.read(sessionId);
            if (finalChunk != null && !finalChunk.isEmpty()) {
                resultOutput.append(finalChunk);
            }
        }

        String output = resultOutput.toString();
        log.info("命令执行完成 sessionId={} outputLength={}", sessionId, output.length());

        return output;
    }

    @Override
    public void resizeTerminal(String sessionId, int cols, int rows) {
        log.debug("调整终端大小 sessionId={} cols={} rows={}", sessionId, cols, rows);

        TerminalSessionEntity entity = sessionCache.get(sessionId);
        if (entity == null || !entity.isActive()) {
            throw new IllegalArgumentException("终端会话不存在或已关闭");
        }

        terminalSessionService.resize(sessionId, cols, rows);

        entity.setCols(cols);
        entity.setRows(rows);
        entity.touch();
    }

    @Override
    public TerminalSessionEntity getTerminalSession(String sessionId) {
        return sessionCache.get(sessionId);
    }

    @Override
    public void closeTerminal(String sessionId) {
        log.info("关闭终端会话 sessionId={}", sessionId);

        TerminalSessionEntity entity = sessionCache.remove(sessionId);
        if (entity != null) {
            terminalSessionService.closeSession(sessionId);
            log.info("终端会话已关闭 sessionId={}", sessionId);
        }
    }

    @Override
    public boolean sessionExists(String sessionId) {
        return sessionCache.containsKey(sessionId);
    }

    @Override
    public String readTerminal(String sessionId) {
        TerminalSessionEntity entity = sessionCache.get(sessionId);
        if (entity == null || !entity.isActive()) {
            throw new IllegalArgumentException("终端会话不存在或已关闭");
        }
        return terminalSessionService.read(sessionId);
    }

    @Override
    public void writeTerminal(String sessionId, String input) {
        TerminalSessionEntity entity = sessionCache.get(sessionId);
        if (entity == null || !entity.isActive()) {
            throw new IllegalArgumentException("终端会话不存在或已关闭");
        }
        terminalSessionService.write(sessionId, input);
        entity.touch();
    }

}