package cn.stackssh.trigger.http;

import cn.stackssh.api.dto.AccessTicketRequestDTO;
import cn.stackssh.api.dto.AccessTicketResponseDTO;
import cn.stackssh.api.dto.CommandApprovalRequestDTO;
import cn.stackssh.api.dto.CommandCheckRequestDTO;
import cn.stackssh.api.dto.CommandCheckResponseDTO;
import cn.stackssh.api.dto.TerminalExecRequestDTO;
import cn.stackssh.api.dto.TerminalExecResponseDTO;
import cn.stackssh.api.dto.TerminalOpenRequestDTO;
import cn.stackssh.api.dto.TerminalOpenResponseDTO;
import cn.stackssh.api.dto.TerminalReadResponseDTO;
import cn.stackssh.api.dto.TerminalResizeRequestDTO;
import cn.stackssh.api.dto.TerminalSessionSummaryDTO;
import cn.stackssh.api.dto.TerminalWriteRequestDTO;
import cn.stackssh.api.response.Response;
import cn.stackssh.domain.ssh.model.entity.TerminalSessionEntity;
import cn.stackssh.domain.ssh.model.valobj.DangerousCommandProperties;
import cn.stackssh.domain.ssh.service.ISshTerminalService;
import cn.stackssh.trigger.support.CurrentUserSupport;
import cn.stackssh.trigger.support.EphemeralAccessTicketService;
import cn.stackssh.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/ssh/terminal")
@CrossOrigin(origins = "*")
public class SshTerminalController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private ISshTerminalService sshTerminalDomainService;

    @Resource
    private DangerousCommandProperties dangerousCommandProperties;

    @Resource
    private CurrentUserSupport currentUserSupport;

    @Resource
    private EphemeralAccessTicketService accessTicketService;

    @PostMapping("open")
    public Response<TerminalOpenResponseDTO> openTerminal(@RequestBody TerminalOpenRequestDTO requestDTO) {
        try {
            String currentUserId = currentUserSupport.requireCurrentUserId();
            currentUserSupport.requireOwnedConnection(requestDTO.getConnectionId());

            int cols = requestDTO.getCols() != null ? requestDTO.getCols() : 120;
            int rows = requestDTO.getRows() != null ? requestDTO.getRows() : 24;
            TerminalSessionEntity entity = sshTerminalDomainService.openTerminal(
                    requestDTO.getConnectionId(), cols, rows, currentUserId);

            TerminalOpenResponseDTO response = TerminalOpenResponseDTO.builder()
                    .sessionId(entity.getSessionId())
                    .connectionId(entity.getConnectionId())
                    .initialOutput(waitForInitialOutput(entity.getSessionId()))
                    .build();
            return success(response);
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("打开终端会话失败 connectionId={}", requestDTO.getConnectionId(), e);
            return error("打开终端失败: " + e.getMessage());
        }
    }

    @PostMapping("exec")
    public Response<TerminalExecResponseDTO> execCommand(@RequestBody TerminalExecRequestDTO requestDTO) {
        try {
            currentUserSupport.requireOwnedTerminalSession(requestDTO.getSessionId());
            String output = sshTerminalDomainService.executeCommand(requestDTO.getSessionId(), requestDTO.getCommand());
            return success(TerminalExecResponseDTO.builder().output(output).build());
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("执行命令失败 sessionId={}", requestDTO.getSessionId(), e);
            return error("执行命令失败: " + e.getMessage());
        }
    }

    @PostMapping("write")
    public Response<Void> writeToTerminal(@RequestBody TerminalWriteRequestDTO requestDTO) {
        try {
            currentUserSupport.requireOwnedTerminalSession(requestDTO.getSessionId());
            sshTerminalDomainService.writeTerminal(requestDTO.getSessionId(), requestDTO.getInput());
            return success(null);
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("写入终端失败 sessionId={}", requestDTO.getSessionId(), e);
            return error("写入终端失败: " + e.getMessage());
        }
    }

    @GetMapping("read")
    public Response<TerminalReadResponseDTO> readFromTerminal(@RequestParam("sessionId") String sessionId) {
        try {
            currentUserSupport.requireOwnedTerminalSession(sessionId);
            String output = sshTerminalDomainService.readTerminal(sessionId);
            return success(TerminalReadResponseDTO.builder().output(output != null ? output : "").build());
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("读取终端失败 sessionId={}", sessionId, e);
            return error("读取终端失败: " + e.getMessage());
        }
    }

    @PostMapping("resize")
    public Response<Void> resizeTerminal(@RequestBody TerminalResizeRequestDTO requestDTO) {
        try {
            currentUserSupport.requireOwnedTerminalSession(requestDTO.getSessionId());
            sshTerminalDomainService.resizeTerminal(requestDTO.getSessionId(), requestDTO.getCols(), requestDTO.getRows());
            return success(null);
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("调整终端大小失败 sessionId={}", requestDTO.getSessionId(), e);
            return error("调整终端大小失败: " + e.getMessage());
        }
    }

    @GetMapping("sessions")
    public Response<List<TerminalSessionSummaryDTO>> listSessions(@RequestParam("connectionId") String connectionId) {
        try {
            String currentUserId = currentUserSupport.requireCurrentUserId();
            currentUserSupport.requireOwnedConnection(connectionId);
            List<TerminalSessionSummaryDTO> sessions = sshTerminalDomainService.listActiveSessions(connectionId, currentUserId)
                    .stream()
                    .map(this::toSummary)
                    .collect(Collectors.toList());
            return success(sessions);
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("查询终端会话失败 connectionId={}", connectionId, e);
            return error("查询终端会话失败: " + e.getMessage());
        }
    }

    @PostMapping("check-command")
    public Response<CommandCheckResponseDTO> checkCommand(@RequestBody CommandCheckRequestDTO requestDTO) {
        try {
            if (requestDTO.getSessionId() != null && !requestDTO.getSessionId().isBlank()) {
                currentUserSupport.requireOwnedTerminalSession(requestDTO.getSessionId());
            }
            String cmd = requestDTO.getCommand() == null ? "" : requestDTO.getCommand().trim();
            for (String pattern : dangerousCommandProperties.getDangerousCommands()) {
                if (pattern != null && !pattern.isBlank() && cmd.contains(pattern.trim())) {
                    return success(CommandCheckResponseDTO.builder()
                            .dangerous(true)
                            .warning("检测到危险命令，请确认后再执行")
                            .matchedPattern(pattern)
                            .build());
                }
            }
            return success(CommandCheckResponseDTO.builder().dangerous(false).build());
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("检测危险命令失败", e);
            return error("检测危险命令失败: " + e.getMessage());
        }
    }

    @PostMapping("approve-command")
    public Response<Void> approveCommand(@RequestBody CommandApprovalRequestDTO requestDTO) {
        try {
            currentUserSupport.requireOwnedTerminalSession(requestDTO.getSessionId());
            sshTerminalDomainService.approveDangerousCommand(requestDTO.getSessionId(), requestDTO.getCommand());
            return success(null);
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("确认危险命令失败 sessionId={}", requestDTO.getSessionId(), e);
            return error("确认危险命令失败: " + e.getMessage());
        }
    }

    @PostMapping("ws-ticket")
    public Response<AccessTicketResponseDTO> issueWsTicket(@RequestBody AccessTicketRequestDTO requestDTO) {
        try {
            String currentUserId = currentUserSupport.requireCurrentUserId();
            currentUserSupport.requireOwnedTerminalSession(requestDTO.getSessionId());
            EphemeralAccessTicketService.TicketRecord record =
                    accessTicketService.issueWebSocketTicket(currentUserId, requestDTO.getSessionId());
            return success(AccessTicketResponseDTO.builder()
                    .ticket(record.getTicket())
                    .expiresInSeconds(60L)
                    .build());
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("签发终端 WS 票据失败 sessionId={}", requestDTO.getSessionId(), e);
            return error("签发终端 WS 票据失败: " + e.getMessage());
        }
    }

    @PostMapping("close")
    public Response<Void> closeTerminal(@RequestParam("sessionId") String sessionId) {
        try {
            currentUserSupport.requireOwnedTerminalSession(sessionId);
            sshTerminalDomainService.closeTerminal(sessionId);
            return success(null);
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("关闭终端会话失败 sessionId={}", sessionId, e);
            return error("关闭终端会话失败: " + e.getMessage());
        }
    }

    private String waitForInitialOutput(String sessionId) {
        String output = sshTerminalDomainService.readTerminal(sessionId);
        if (output == null || output.isEmpty()) {
            return "";
        }
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String more = sshTerminalDomainService.readTerminal(sessionId);
        return more == null || more.isEmpty() ? output : output + more;
    }

    private TerminalSessionSummaryDTO toSummary(TerminalSessionEntity entity) {
        return TerminalSessionSummaryDTO.builder()
                .sessionId(entity.getSessionId())
                .connectionId(entity.getConnectionId())
                .cols(entity.getCols())
                .rows(entity.getRows())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(FMT) : null)
                .lastActiveAt(entity.getLastActiveAt() != null ? entity.getLastActiveAt().format(FMT) : null)
                .build();
    }

    private <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(data)
                .build();
    }

    private <T> Response<T> illegal(String message) {
        return Response.<T>builder()
                .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                .info(message)
                .build();
    }

    private <T> Response<T> error(String message) {
        return Response.<T>builder()
                .code(ResponseCode.UN_ERROR.getCode())
                .info(message)
                .build();
    }
}
