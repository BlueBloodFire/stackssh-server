package cn.stackssh.trigger.http;

import cn.stackssh.api.ISshConnectionService;
import cn.stackssh.api.dto.SshConnectionRequestDTO;
import cn.stackssh.api.dto.SshConnectionResponseDTO;
import cn.stackssh.api.response.Response;
import cn.stackssh.domain.ssh.model.entity.SshConnectionConfigEntity;
import cn.stackssh.domain.ssh.model.entity.SshConnectionEntity;
import cn.stackssh.domain.ssh.model.valobj.AuthTypeEnum;
import cn.stackssh.domain.ssh.model.valobj.ConnectionStatusEnum;
import cn.stackssh.domain.ssh.service.ISshConnectionDomainService;
import cn.stackssh.trigger.support.CurrentUserSupport;
import cn.stackssh.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/ssh")
@CrossOrigin(origins = "*")
public class SshConnectionController implements ISshConnectionService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private ISshConnectionDomainService sshConnectionDomainService;

    @Resource
    private CurrentUserSupport currentUserSupport;

    @RequestMapping(value = "create_connection", method = RequestMethod.POST)
    @Override
    public Response<SshConnectionResponseDTO> createConnection(@RequestBody SshConnectionRequestDTO requestDTO) {
        try {
            String currentUserId = currentUserSupport.requireCurrentUserId();
            SshConnectionEntity entity = toEntity(requestDTO, currentUserId);
            SshConnectionConfigEntity configEntity = toConfigEntity(requestDTO);
            sshConnectionDomainService.createConnection(entity, configEntity);
            return success(toResponseDTO(entity));
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("创建SSH连接失败", e);
            return error(ResponseCode.UN_ERROR.getInfo());
        }
    }

    @RequestMapping(value = "update_connection", method = RequestMethod.POST)
    @Override
    public Response<SshConnectionResponseDTO> updateConnection(@RequestBody SshConnectionRequestDTO requestDTO) {
        try {
            String currentUserId = currentUserSupport.requireCurrentUserId();
            currentUserSupport.requireOwnedConnection(requestDTO.getConnectionId());
            SshConnectionEntity entity = toEntity(requestDTO, currentUserId);
            sshConnectionDomainService.updateConnection(entity, toConfigEntity(requestDTO));
            return success(toResponseDTO(sshConnectionDomainService.getConnection(entity.getConnectionId())));
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("更新SSH连接失败 connectionId={}", requestDTO.getConnectionId(), e);
            return error(ResponseCode.UN_ERROR.getInfo());
        }
    }

    @RequestMapping(value = "delete_connection", method = RequestMethod.POST)
    @Override
    public Response<Void> deleteConnection(@RequestParam("connectionId") String connectionId) {
        try {
            currentUserSupport.requireOwnedConnection(connectionId);
            sshConnectionDomainService.deleteConnection(connectionId);
            return success(null);
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("删除SSH连接失败 connectionId={}", connectionId, e);
            return error(ResponseCode.UN_ERROR.getInfo());
        }
    }

    @RequestMapping(value = "get_connection", method = RequestMethod.GET)
    @Override
    public Response<SshConnectionResponseDTO> getConnection(@RequestParam("connectionId") String connectionId) {
        try {
            SshConnectionEntity entity = currentUserSupport.requireOwnedConnection(connectionId);
            return success(toResponseDTO(entity));
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("查询SSH连接失败 connectionId={}", connectionId, e);
            return error(ResponseCode.UN_ERROR.getInfo());
        }
    }

    @RequestMapping(value = "connection_list", method = RequestMethod.GET)
    @Override
    public Response<List<SshConnectionResponseDTO>> getConnectionList(@RequestParam(value = "userId", defaultValue = "default") String userId) {
        try {
            String currentUserId = currentUserSupport.requireCurrentUserId();
            List<SshConnectionResponseDTO> dtoList = sshConnectionDomainService.getConnectionList(currentUserId).stream()
                    .map(entity -> {
                        boolean actuallyConnected = sshConnectionDomainService.isConnected(entity.getConnectionId());
                        if (actuallyConnected && entity.getStatus() != ConnectionStatusEnum.CONNECTED) {
                            entity.setStatus(ConnectionStatusEnum.CONNECTED);
                        } else if (!actuallyConnected && entity.getStatus() == ConnectionStatusEnum.CONNECTED) {
                            entity.setStatus(ConnectionStatusEnum.DISCONNECTED);
                        }
                        return toResponseDTO(entity);
                    })
                    .collect(Collectors.toList());
            return success(dtoList);
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("查询SSH连接列表失败", e);
            return error(ResponseCode.UN_ERROR.getInfo());
        }
    }

    @RequestMapping(value = "connect", method = RequestMethod.POST)
    @Override
    public Response<Void> connect(@RequestParam("connectionId") String connectionId) {
        try {
            currentUserSupport.requireOwnedConnection(connectionId);
            boolean success = sshConnectionDomainService.connect(connectionId);
            return success
                    ? success(null, "连接成功")
                    : error("连接失败，请检查主机地址、端口和认证信息");
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("建立SSH连接失败 connectionId={}", connectionId, e);
            return error("连接失败: " + e.getMessage());
        }
    }

    @RequestMapping(value = "disconnect", method = RequestMethod.POST)
    @Override
    public Response<Void> disconnect(@RequestParam("connectionId") String connectionId) {
        try {
            currentUserSupport.requireOwnedConnection(connectionId);
            sshConnectionDomainService.disconnect(connectionId);
            return success(null, "已断开连接");
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("断开SSH连接失败 connectionId={}", connectionId, e);
            return error("断开连接失败: " + e.getMessage());
        }
    }

    private SshConnectionEntity toEntity(SshConnectionRequestDTO dto, String currentUserId) {
        return SshConnectionEntity.builder()
                .connectionId(dto.getConnectionId())
                .connectionName(dto.getConnectionName())
                .host(dto.getHost())
                .port(dto.getPort())
                .username(dto.getUsername())
                .authType(dto.getAuthType() != null ? AuthTypeEnum.fromCode(dto.getAuthType()) : AuthTypeEnum.PASSWORD)
                .password(dto.getPassword())
                .privateKey(dto.getPrivateKey())
                .userId(currentUserId)
                .build();
    }

    private SshConnectionConfigEntity toConfigEntity(SshConnectionRequestDTO dto) {
        return SshConnectionConfigEntity.builder()
                .connectTimeout(dto.getConnectTimeout())
                .keepaliveInterval(dto.getKeepaliveInterval())
                .startupCommand(dto.getStartupCommand())
                .compression(dto.getCompression())
                .strictHostKeyCheck(dto.getStrictHostKeyCheck())
                .build();
    }

    private SshConnectionResponseDTO toResponseDTO(SshConnectionEntity entity) {
        return SshConnectionResponseDTO.builder()
                .connectionId(entity.getConnectionId())
                .connectionName(entity.getConnectionName())
                .host(entity.getHost())
                .port(entity.getPort())
                .username(entity.getUsername())
                .authType(entity.getAuthType() != null ? entity.getAuthType().getCode() : null)
                .status(entity.getStatus() != null ? entity.getStatus().getCode() : null)
                .encrypted(entity.getEncrypted())
                .userId(entity.getUserId())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(FMT) : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().format(FMT) : null)
                .build();
    }

    private <T> Response<T> success(T data) {
        return success(data, ResponseCode.SUCCESS.getInfo());
    }

    private <T> Response<T> success(T data, String info) {
        return Response.<T>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(info)
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
