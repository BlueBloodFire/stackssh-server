package cn.stackssh.trigger.http;

import cn.stackssh.api.ISshFileService;
import cn.stackssh.api.dto.AccessTicketRequestDTO;
import cn.stackssh.api.dto.AccessTicketResponseDTO;
import cn.stackssh.api.dto.SshFileContentResponseDTO;
import cn.stackssh.api.dto.SshFileEntryDTO;
import cn.stackssh.api.dto.SshFileTreeResponseDTO;
import cn.stackssh.api.response.Response;
import cn.stackssh.domain.ssh.model.entity.SshFileContentEntity;
import cn.stackssh.domain.ssh.model.entity.SshFileEntryEntity;
import cn.stackssh.domain.ssh.model.entity.SshFileTreeEntity;
import cn.stackssh.domain.ssh.service.ISshFileDomainService;
import cn.stackssh.trigger.support.CurrentUserSupport;
import cn.stackssh.trigger.support.EphemeralAccessTicketService;
import cn.stackssh.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/ssh/file")
@CrossOrigin(origins = "*")
public class SshFileController implements ISshFileService {

    @Resource
    private ISshFileDomainService sshFileDomainService;

    @Resource
    private CurrentUserSupport currentUserSupport;

    @Resource
    private EphemeralAccessTicketService accessTicketService;

    @RequestMapping(value = "tree", method = RequestMethod.GET)
    @Override
    public Response<SshFileTreeResponseDTO> tree(@RequestParam("connectionId") String connectionId,
                                                 @RequestParam(value = "path", required = false) String path) {
        try {
            currentUserSupport.requireOwnedConnection(connectionId);
            return success(toTreeDTO(sshFileDomainService.tree(connectionId, path)));
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("查询目录异常 connectionId={} path={}", connectionId, path, e);
            return error("查询目录失败: " + e.getMessage());
        }
    }

    @RequestMapping(value = "content", method = RequestMethod.GET)
    @Override
    public Response<SshFileContentResponseDTO> content(@RequestParam("connectionId") String connectionId,
                                                       @RequestParam("path") String path) {
        try {
            currentUserSupport.requireOwnedConnection(connectionId);
            return success(toContentDTO(sshFileDomainService.content(connectionId, path)));
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("读取文件异常 connectionId={} path={}", connectionId, path, e);
            return error("读取文件失败: " + e.getMessage());
        }
    }

    @RequestMapping(value = "content-chunk", method = RequestMethod.GET)
    public Response<SshFileContentResponseDTO> contentChunk(@RequestParam("connectionId") String connectionId,
                                                            @RequestParam("path") String path,
                                                            @RequestParam(value = "offset", required = false) Long offset,
                                                            @RequestParam(value = "limit", required = false) Integer limit) {
        try {
            currentUserSupport.requireOwnedConnection(connectionId);
            return success(toContentDTO(sshFileDomainService.content(connectionId, path, offset, limit)));
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("分片读取文件异常 connectionId={} path={} offset={}", connectionId, path, offset, e);
            return error("读取文件失败: " + e.getMessage());
        }
    }

    @RequestMapping(value = "create-file", method = RequestMethod.POST)
    public Response<Void> createFile(@RequestParam("connectionId") String connectionId,
                                     @RequestParam("path") String path,
                                     @RequestParam(value = "sudo", defaultValue = "false") boolean sudo) {
        try {
            currentUserSupport.requireOwnedConnection(connectionId);
            sshFileDomainService.createFile(connectionId, path, sudo);
            return success(null, "创建文件成功");
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("创建文件异常 connectionId={} path={}", connectionId, path, e);
            return error("创建文件失败: " + e.getMessage());
        }
    }

    @RequestMapping(value = "create-directory", method = RequestMethod.POST)
    public Response<Void> createDirectory(@RequestParam("connectionId") String connectionId,
                                          @RequestParam("path") String path,
                                          @RequestParam(value = "sudo", defaultValue = "false") boolean sudo) {
        try {
            currentUserSupport.requireOwnedConnection(connectionId);
            sshFileDomainService.createDirectory(connectionId, path, sudo);
            return success(null, "创建目录成功");
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("创建目录异常 connectionId={} path={}", connectionId, path, e);
            return error("创建目录失败: " + e.getMessage());
        }
    }

    @RequestMapping(value = "rename", method = RequestMethod.POST)
    public Response<Void> rename(@RequestParam("connectionId") String connectionId,
                                 @RequestParam("oldPath") String oldPath,
                                 @RequestParam("newPath") String newPath,
                                 @RequestParam(value = "sudo", defaultValue = "false") boolean sudo) {
        try {
            currentUserSupport.requireOwnedConnection(connectionId);
            sshFileDomainService.rename(connectionId, oldPath, newPath, sudo);
            return success(null, "重命名成功");
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("重命名异常 connectionId={} oldPath={} newPath={}", connectionId, oldPath, newPath, e);
            return error("重命名失败: " + e.getMessage());
        }
    }

    @RequestMapping(value = "delete", method = RequestMethod.POST)
    public Response<Void> delete(@RequestParam("connectionId") String connectionId,
                                 @RequestParam("path") String path,
                                 @RequestParam(value = "sudo", defaultValue = "false") boolean sudo) {
        try {
            currentUserSupport.requireOwnedConnection(connectionId);
            sshFileDomainService.delete(connectionId, path, sudo);
            return success(null, "删除成功");
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("删除异常 connectionId={} path={}", connectionId, path, e);
            return error("删除失败: " + e.getMessage());
        }
    }

    @RequestMapping(value = "save-content", method = RequestMethod.POST)
    public Response<Void> saveContent(@RequestParam("connectionId") String connectionId,
                                      @RequestParam("path") String path,
                                      @RequestParam(value = "sudo", defaultValue = "false") boolean sudo,
                                      @RequestBody Map<String, String> body) {
        try {
            currentUserSupport.requireOwnedConnection(connectionId);
            sshFileDomainService.saveFile(connectionId, path, body.getOrDefault("content", ""), sudo);
            return success(null, "保存文件成功");
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("保存文件异常 connectionId={} path={}", connectionId, path, e);
            return error("保存文件失败: " + e.getMessage());
        }
    }

    @RequestMapping(value = "upload", method = RequestMethod.POST)
    public Response<Void> upload(@RequestParam("connectionId") String connectionId,
                                 @RequestParam("path") String path,
                                 @RequestParam("file") MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            currentUserSupport.requireOwnedConnection(connectionId);
            sshFileDomainService.uploadFile(connectionId, path, inputStream);
            return success(null, "上传文件成功");
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("上传文件异常 connectionId={} path={}", connectionId, path, e);
            return error("上传文件失败: " + e.getMessage());
        }
    }

    @RequestMapping(value = "download-ticket", method = RequestMethod.POST)
    public Response<AccessTicketResponseDTO> issueDownloadTicket(@RequestBody AccessTicketRequestDTO requestDTO) {
        try {
            String currentUserId = currentUserSupport.requireCurrentUserId();
            currentUserSupport.requireOwnedConnection(requestDTO.getConnectionId());
            EphemeralAccessTicketService.TicketRecord record =
                    accessTicketService.issueDownloadTicket(currentUserId, requestDTO.getConnectionId(), requestDTO.getPath());
            return success(AccessTicketResponseDTO.builder()
                    .ticket(record.getTicket())
                    .expiresInSeconds(60L)
                    .build());
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("签发下载票据失败 connectionId={} path={}", requestDTO.getConnectionId(), requestDTO.getPath(), e);
            return error("签发下载票据失败: " + e.getMessage());
        }
    }

    @RequestMapping(value = "download", method = RequestMethod.GET)
    public void download(@RequestParam("connectionId") String connectionId,
                         @RequestParam("path") String path,
                         @RequestParam("ticket") String ticket,
                         HttpServletResponse response) {
        try {
            accessTicketService.consumeDownloadTicket(ticket, connectionId, path);
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));
            try (OutputStream outputStream = response.getOutputStream()) {
                sshFileDomainService.downloadFile(connectionId, path, outputStream);
            }
        } catch (Exception e) {
            log.error("下载文件异常 connectionId={} path={}", connectionId, path, e);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private static SshFileTreeResponseDTO toTreeDTO(SshFileTreeEntity entity) {
        List<SshFileEntryDTO> items = entity.getItems() == null
                ? Collections.emptyList()
                : entity.getItems().stream().map(SshFileController::toEntryDTO).collect(Collectors.toList());
        return SshFileTreeResponseDTO.builder()
                .rootPath(entity.getRootPath())
                .homePath(entity.getHomePath())
                .currentPath(entity.getCurrentPath())
                .parentPath(entity.getParentPath())
                .items(items)
                .build();
    }

    private static SshFileEntryDTO toEntryDTO(SshFileEntryEntity item) {
        return SshFileEntryDTO.builder()
                .name(item.getName())
                .path(item.getPath())
                .directory(item.isDirectory())
                .size(item.getSize())
                .modifiedAt(item.getModifiedAt())
                .build();
    }

    private static SshFileContentResponseDTO toContentDTO(SshFileContentEntity entity) {
        return SshFileContentResponseDTO.builder()
                .path(entity.getPath())
                .name(entity.getName())
                .charset(entity.getCharset())
                .size(entity.getSize())
                .binary(entity.isBinary())
                .truncated(entity.isTruncated())
                .offset(entity.getOffset())
                .remaining(entity.getOffset() != null && entity.getSize() != null
                        ? Math.max(0, entity.getSize() - entity.getOffset() - entity.getContent().getBytes(StandardCharsets.UTF_8).length)
                        : null)
                .content(entity.getContent())
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
