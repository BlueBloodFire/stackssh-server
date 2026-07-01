package cn.stackssh.domain.ssh.service.terminal;

import cn.stackssh.domain.ssh.adapter.port.ISshSessionPort;
import cn.stackssh.domain.ssh.adapter.port.ITerminalRecordingPort;
import cn.stackssh.domain.ssh.adapter.port.ITerminalSessionPort;
import cn.stackssh.domain.ssh.model.entity.TerminalRecordingEntity;
import cn.stackssh.domain.ssh.model.entity.TerminalSessionEntity;
import cn.stackssh.domain.ssh.model.valobj.DangerousCommandProperties;
import cn.stackssh.domain.ssh.service.ISshTerminalService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SshTerminalService implements ISshTerminalService {

    private static final long COMMAND_EXEC_WAIT_MS = 5000;
    private static final long COMMAND_EXEC_CHECK_INTERVAL_MS = 100;
    private static final long DANGEROUS_APPROVAL_TTL_SECONDS = 30;

    private final ISshSessionPort sshSessionService;
    private final ITerminalSessionPort terminalSessionService;
    private final ITerminalRecordingPort recordingPort;
    private final DangerousCommandProperties dangerousCommandProperties;

    private final Map<String, TerminalSessionEntity> sessionCache = new ConcurrentHashMap<>();
    private final Map<String, Long> recordingDbIdCache = new ConcurrentHashMap<>();
    private final Map<String, Long> recordingStartMs = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> commandBufferCache = new ConcurrentHashMap<>();
    private final Map<String, DangerousApproval> dangerousApprovalCache = new ConcurrentHashMap<>();

    public SshTerminalService(ISshSessionPort sshSessionService,
                              ITerminalSessionPort terminalSessionService,
                              ITerminalRecordingPort recordingPort,
                              DangerousCommandProperties dangerousCommandProperties) {
        this.sshSessionService = sshSessionService;
        this.terminalSessionService = terminalSessionService;
        this.recordingPort = recordingPort;
        this.dangerousCommandProperties = dangerousCommandProperties;
    }

    @Override
    public TerminalSessionEntity openTerminal(String connectionId, int cols, int rows, String ownerUserId) {
        log.info("打开终端会话 connectionId={} cols={} rows={} ownerUserId={}", connectionId, cols, rows, ownerUserId);

        if (!sshSessionService.isConnected(connectionId)) {
            throw new IllegalStateException("SSH连接未建立，请先连接");
        }

        String sessionId = terminalSessionService.openTerminal(connectionId, cols, rows);
        TerminalSessionEntity entity = TerminalSessionEntity.builder()
                .sessionId(sessionId)
                .connectionId(connectionId)
                .ownerUserId(ownerUserId)
                .cols(cols)
                .rows(rows)
                .status(1)
                .createdAt(LocalDateTime.now())
                .lastActiveAt(LocalDateTime.now())
                .build();

        sessionCache.put(sessionId, entity);
        commandBufferCache.put(sessionId, new StringBuilder());
        log.info("[Diag] terminal session cached sessionId={} ownerUserId={} cacheSize={}",
                sessionId, ownerUserId, sessionCache.size());
        return entity;
    }

    @Override
    public String executeCommand(String sessionId, String command) {
        TerminalSessionEntity entity = requireActiveSession(sessionId);
        String normalizedCommand = normalizeCommand(command);
        if (isDangerousCommand(normalizedCommand) && !consumeDangerousApproval(sessionId, normalizedCommand)) {
            throw new IllegalArgumentException("检测到危险命令，需在前端确认后才能执行");
        }

        log.info("Agent执行命令 sessionId={} ownerUserId={} command={}", sessionId, entity.getOwnerUserId(), normalizedCommand);
        terminalSessionService.setAgentCapture(sessionId, true);

        try {
            terminalSessionService.readAgentBuffer(sessionId);
            terminalSessionService.write(sessionId, normalizedCommand + "\n");
            entity.touch();

            long deadline = System.currentTimeMillis() + COMMAND_EXEC_WAIT_MS;
            StringBuilder resultOutput = new StringBuilder();
            int emptyReadCount = 0;
            final int emptyReadThreshold = 3;

            while (System.currentTimeMillis() < deadline) {
                String chunk = terminalSessionService.readAgentBuffer(sessionId);
                if (chunk != null && !chunk.isEmpty()) {
                    resultOutput.append(chunk);
                    emptyReadCount = 0;
                } else {
                    emptyReadCount++;
                    String current = resultOutput.toString();
                    if (emptyReadCount >= emptyReadThreshold
                            && current.length() > 0
                            && (current.matches(".*[#$][\\s\\r\\n].*")
                            || current.contains("\r\n") && current.split("\r\n").length > 2)) {
                        break;
                    }
                }

                try {
                    Thread.sleep(COMMAND_EXEC_CHECK_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            String finalChunk = terminalSessionService.readAgentBuffer(sessionId);
            if (finalChunk != null && !finalChunk.isEmpty()) {
                resultOutput.append(finalChunk);
            }

            String output = resultOutput.toString();
            log.info("命令执行完成 sessionId={} outputLength={}", sessionId, output.length());
            return output;
        } finally {
            terminalSessionService.setAgentCapture(sessionId, false);
        }
    }

    @Override
    public void resizeTerminal(String sessionId, int cols, int rows) {
        TerminalSessionEntity entity = requireActiveSession(sessionId);
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
    public boolean isSessionOwner(String sessionId, String ownerUserId) {
        TerminalSessionEntity entity = sessionCache.get(sessionId);
        return entity != null && ownerUserId != null && ownerUserId.equals(entity.getOwnerUserId());
    }

    @Override
    public void closeTerminal(String sessionId) {
        log.info("关闭终端会话 sessionId={}", sessionId);
        TerminalSessionEntity entity = sessionCache.remove(sessionId);
        commandBufferCache.remove(sessionId);
        dangerousApprovalCache.remove(sessionId);
        if (entity != null) {
            terminalSessionService.closeSession(sessionId);
        }
    }

    @Override
    public boolean sessionExists(String sessionId) {
        TerminalSessionEntity entity = sessionCache.get(sessionId);
        if (entity == null) {
            return false;
        }
        boolean channelAlive = terminalSessionService.sessionExists(sessionId);
        if (!channelAlive) {
            closeTerminal(sessionId);
            return false;
        }
        return true;
    }

    @Override
    public String readTerminal(String sessionId) {
        requireActiveSession(sessionId);
        return terminalSessionService.read(sessionId);
    }

    @Override
    public void writeTerminal(String sessionId, String input) {
        TerminalSessionEntity entity = requireActiveSession(sessionId);
        validateInteractiveInput(sessionId, input);
        terminalSessionService.write(sessionId, input);
        entity.touch();
    }

    @Override
    public List<TerminalSessionEntity> listActiveSessions(String connectionId, String ownerUserId) {
        return sessionCache.values().stream()
                .filter(TerminalSessionEntity::isActive)
                .filter(session -> ownerUserId != null && ownerUserId.equals(session.getOwnerUserId()))
                .filter(session -> connectionId == null || connectionId.equals(session.getConnectionId()))
                .filter(session -> sessionExists(session.getSessionId()))
                .collect(Collectors.toList());
    }

    @Override
    public void approveDangerousCommand(String sessionId, String command) {
        requireActiveSession(sessionId);
        String normalizedCommand = normalizeCommand(command);
        if (!isDangerousCommand(normalizedCommand)) {
            dangerousApprovalCache.remove(sessionId);
            return;
        }
        dangerousApprovalCache.put(sessionId, DangerousApproval.builder()
                .command(normalizedCommand)
                .expiresAt(Instant.now().plusSeconds(DANGEROUS_APPROVAL_TTL_SECONDS))
                .build());
    }

    @Override
    public void closeTerminalsByConnection(String connectionId) {
        List<String> toClose = new ArrayList<>();
        for (Map.Entry<String, TerminalSessionEntity> entry : sessionCache.entrySet()) {
            if (connectionId.equals(entry.getValue().getConnectionId())) {
                toClose.add(entry.getKey());
            }
        }
        for (String sid : toClose) {
            log.info("SSH断开，关闭终端 sessionId={} connectionId={}", sid, connectionId);
            try {
                terminalSessionService.closeSession(sid);
            } catch (Exception ignored) {
            }
            sessionCache.remove(sid);
            commandBufferCache.remove(sid);
            dangerousApprovalCache.remove(sid);
        }
    }

    @Override
    public String startRecording(String sessionId, String connectionId) {
        TerminalSessionEntity entity = requireActiveSession(sessionId);
        String recordingId = UUID.randomUUID().toString().replace("-", "");
        long dbId = recordingPort.createRecording(recordingId, connectionId, sessionId, entity.getCols(), entity.getRows());
        recordingDbIdCache.put(recordingId, dbId);
        recordingStartMs.put(recordingId, System.currentTimeMillis());
        terminalSessionService.startRecording(sessionId);
        log.info("开始录制 recordingId={} sessionId={}", recordingId, sessionId);
        return recordingId;
    }

    @Override
    public void stopRecording(String sessionId, String recordingId) {
        requireActiveSession(sessionId);
        Long dbId = recordingDbIdCache.remove(recordingId);
        Long startMs = recordingStartMs.remove(recordingId);
        if (dbId == null) {
            throw new IllegalArgumentException("录制不存在或已停止 recordingId=" + recordingId);
        }
        List<TerminalRecordingEntity.Event> events = terminalSessionService.stopRecording(sessionId);
        long durationMs = startMs == null ? 0 : System.currentTimeMillis() - startMs;
        recordingPort.saveEvents(dbId, events);
        recordingPort.finishRecording(recordingId, 1, durationMs);
        log.info("录制已停止并持久化 recordingId={} events={} durationMs={}", recordingId, events.size(), durationMs);
    }

    @Override
    public List<TerminalRecordingEntity> listRecordings(String connectionId) {
        return recordingPort.listByConnectionId(connectionId);
    }

    @Override
    public TerminalRecordingEntity getRecordingPlayback(String recordingId) {
        return recordingPort.getRecordingWithEvents(recordingId);
    }

    private TerminalSessionEntity requireActiveSession(String sessionId) {
        TerminalSessionEntity entity = sessionCache.get(sessionId);
        if (entity == null || !entity.isActive()) {
            throw new IllegalArgumentException("终端会话不存在或已关闭");
        }
        if (!terminalSessionService.sessionExists(sessionId)) {
            closeTerminal(sessionId);
            throw new IllegalArgumentException("终端会话已失效，请重新连接");
        }
        return entity;
    }

    private void validateInteractiveInput(String sessionId, String input) {
        if (input == null || input.isEmpty()) {
            return;
        }
        if (input.indexOf('\u001b') >= 0) {
            return;
        }

        StringBuilder buffer = commandBufferCache.computeIfAbsent(sessionId, key -> new StringBuilder());
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (current == '\r' || current == '\n') {
                String command = normalizeCommand(buffer.toString());
                buffer.setLength(0);
                if (isDangerousCommand(command) && !consumeDangerousApproval(sessionId, command)) {
                    throw new IllegalArgumentException("检测到危险命令，需在前端确认后才能执行");
                }
                continue;
            }

            if (current == '\b' || current == 127) {
                if (buffer.length() > 0) {
                    buffer.deleteCharAt(buffer.length() - 1);
                }
                continue;
            }

            if (current == 3 || current == 21 || current == 24) {
                buffer.setLength(0);
                continue;
            }

            if (!Character.isISOControl(current)) {
                buffer.append(current);
            }
        }
    }

    private boolean isDangerousCommand(String command) {
        String normalized = normalizeCommand(command);
        if (normalized.isEmpty() || dangerousCommandProperties.getDangerousCommands() == null) {
            return false;
        }
        for (String pattern : dangerousCommandProperties.getDangerousCommands()) {
            if (pattern != null && !pattern.isBlank() && normalized.contains(pattern.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean consumeDangerousApproval(String sessionId, String command) {
        DangerousApproval approval = dangerousApprovalCache.get(sessionId);
        if (approval == null) {
            return false;
        }
        if (approval.getExpiresAt().isBefore(Instant.now())) {
            dangerousApprovalCache.remove(sessionId);
            return false;
        }
        if (!approval.getCommand().equals(normalizeCommand(command))) {
            return false;
        }
        dangerousApprovalCache.remove(sessionId);
        return true;
    }

    private String normalizeCommand(String command) {
        return command == null ? "" : command.trim();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class DangerousApproval {
        private String command;
        private Instant expiresAt;
    }
}
