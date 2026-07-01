package cn.stackssh.trigger.support;

import cn.stackssh.domain.ssh.model.entity.SshConnectionEntity;
import cn.stackssh.domain.ssh.model.entity.TerminalRecordingEntity;
import cn.stackssh.domain.ssh.model.entity.TerminalSessionEntity;
import cn.stackssh.domain.ssh.service.ISshConnectionDomainService;
import cn.stackssh.domain.ssh.service.ISshTerminalService;
import jakarta.annotation.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserSupport {

    @Resource
    private ISshConnectionDomainService sshConnectionDomainService;

    @Resource
    private ISshTerminalService sshTerminalService;

    public String requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("未登录或登录已失效");
        }

        String principal = authentication.getPrincipal().toString();
        int splitIndex = principal.indexOf(':');
        return splitIndex >= 0 ? principal.substring(0, splitIndex) : principal;
    }

    public SshConnectionEntity requireOwnedConnection(String connectionId) {
        String currentUserId = requireCurrentUserId();
        SshConnectionEntity connection = sshConnectionDomainService.getConnection(connectionId);
        if (connection == null) {
            throw new IllegalArgumentException("连接不存在");
        }
        if (!currentUserId.equals(connection.getUserId())) {
            throw new AccessDeniedException("无权访问该 SSH 连接");
        }
        return connection;
    }

    public TerminalSessionEntity requireOwnedTerminalSession(String sessionId) {
        String currentUserId = requireCurrentUserId();
        TerminalSessionEntity session = sshTerminalService.getTerminalSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("终端会话不存在");
        }
        if (!currentUserId.equals(session.getOwnerUserId())) {
            throw new AccessDeniedException("无权访问该终端会话");
        }
        return session;
    }

    public void requireOwnedRecording(TerminalRecordingEntity recording) {
        if (recording == null) {
            throw new IllegalArgumentException("录制不存在");
        }
        requireOwnedConnection(recording.getConnectionId());
    }
}
