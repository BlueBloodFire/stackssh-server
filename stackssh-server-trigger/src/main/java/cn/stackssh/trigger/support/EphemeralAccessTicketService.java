package cn.stackssh.trigger.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EphemeralAccessTicketService {

    private static final long DEFAULT_TTL_SECONDS = 60;

    private final ConcurrentHashMap<String, TicketRecord> tickets = new ConcurrentHashMap<>();

    public TicketRecord issueWebSocketTicket(String userId, String sessionId) {
        return issue(TicketType.TERMINAL_WS, userId, sessionId, null, null);
    }

    public TicketRecord issueDownloadTicket(String userId, String connectionId, String path) {
        return issue(TicketType.FILE_DOWNLOAD, userId, null, connectionId, path);
    }

    public TicketRecord consumeWebSocketTicket(String ticket, String sessionId) {
        TicketRecord record = consume(ticket, TicketType.TERMINAL_WS);
        if (!sessionId.equals(record.getSessionId())) {
            throw new IllegalArgumentException("票据与终端会话不匹配");
        }
        return record;
    }

    public TicketRecord consumeDownloadTicket(String ticket, String connectionId, String path) {
        TicketRecord record = consume(ticket, TicketType.FILE_DOWNLOAD);
        if (!connectionId.equals(record.getConnectionId()) || !path.equals(record.getPath())) {
            throw new IllegalArgumentException("票据与下载资源不匹配");
        }
        return record;
    }

    private TicketRecord issue(TicketType type, String userId, String sessionId, String connectionId, String path) {
        cleanupExpired();
        String ticket = UUID.randomUUID().toString().replace("-", "");
        TicketRecord record = TicketRecord.builder()
                .ticket(ticket)
                .type(type)
                .userId(userId)
                .sessionId(sessionId)
                .connectionId(connectionId)
                .path(path)
                .expiresAt(Instant.now().plusSeconds(DEFAULT_TTL_SECONDS))
                .build();
        tickets.put(ticket, record);
        return record;
    }

    private TicketRecord consume(String ticket, TicketType expectedType) {
        if (ticket == null || ticket.isBlank()) {
            throw new IllegalArgumentException("访问票据缺失");
        }
        cleanupExpired();
        TicketRecord record = tickets.remove(ticket);
        if (record == null || record.getType() != expectedType) {
            throw new IllegalArgumentException("访问票据无效或已过期");
        }
        if (record.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("访问票据已过期");
        }
        return record;
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        tickets.entrySet().removeIf(entry -> entry.getValue().getExpiresAt().isBefore(now));
    }

    public enum TicketType {
        TERMINAL_WS,
        FILE_DOWNLOAD
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketRecord {
        private String ticket;
        private TicketType type;
        private String userId;
        private String sessionId;
        private String connectionId;
        private String path;
        private Instant expiresAt;
    }
}
