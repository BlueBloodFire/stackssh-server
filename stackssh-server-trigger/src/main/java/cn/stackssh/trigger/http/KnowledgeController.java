package cn.stackssh.trigger.http;

import cn.stackssh.api.dto.KnowledgeDocumentDTO;
import cn.stackssh.api.dto.KnowledgeSearchRequestDTO;
import cn.stackssh.api.response.Response;
import cn.stackssh.domain.knowledge.model.entity.KnowledgeDocumentEntity;
import cn.stackssh.domain.knowledge.service.IKnowledgeService;
import cn.stackssh.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge")
@CrossOrigin(origins = "*")
public class KnowledgeController {

    @Resource
    private IKnowledgeService knowledgeService;

    @PostMapping("/upload")
    public Response<KnowledgeDocumentDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "connectionId", required = false) String connectionId,
            @RequestParam(value = "agentId", required = false) String agentId) {
        try {
            log.info("知识库上传 file={} connectionId={} agentId={}", file.getOriginalFilename(), connectionId, agentId);
            KnowledgeDocumentEntity entity = knowledgeService.uploadDocument(file, connectionId, agentId);
            return Response.<KnowledgeDocumentDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(toDTO(entity))
                    .build();
        } catch (Exception e) {
            log.error("知识库上传失败", e);
            return Response.<KnowledgeDocumentDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }

    @GetMapping("/list")
    public Response<List<KnowledgeDocumentDTO>> list(
            @RequestParam(value = "connectionId", required = false) String connectionId,
            @RequestParam(value = "agentId", required = false) String agentId) {
        try {
            List<KnowledgeDocumentDTO> list = knowledgeService.listDocuments(connectionId, agentId)
                    .stream().map(this::toDTO).collect(Collectors.toList());
            return Response.<List<KnowledgeDocumentDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(list)
                    .build();
        } catch (Exception e) {
            log.error("知识库列表查询失败", e);
            return Response.<List<KnowledgeDocumentDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }

    @DeleteMapping("/{docId}")
    public Response<Void> delete(@PathVariable("docId") String docId) {
        try {
            log.info("知识库删除 docId={}", docId);
            knowledgeService.deleteDocument(docId);
            return Response.<Void>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("知识库删除失败 docId={}", docId, e);
            return Response.<Void>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }

    @PostMapping("/search")
    public Response<List<String>> search(@RequestBody KnowledgeSearchRequestDTO request) {
        try {
            int topK = request.getTopK() != null ? request.getTopK() : 3;
            List<String> results = knowledgeService.searchRelevant(request.getQuery(), request.getConnectionId(), topK);
            return Response.<List<String>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(results)
                    .build();
        } catch (Exception e) {
            log.error("知识库检索失败", e);
            return Response.<List<String>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }

    private KnowledgeDocumentDTO toDTO(KnowledgeDocumentEntity entity) {
        KnowledgeDocumentDTO dto = new KnowledgeDocumentDTO();
        dto.setDocId(entity.getDocId());
        dto.setConnectionId(entity.getConnectionId());
        dto.setAgentId(entity.getAgentId());
        dto.setName(entity.getName());
        dto.setFileType(entity.getFileType());
        dto.setStatus(entity.getStatus());
        dto.setChunkCount(entity.getChunkCount());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

}
