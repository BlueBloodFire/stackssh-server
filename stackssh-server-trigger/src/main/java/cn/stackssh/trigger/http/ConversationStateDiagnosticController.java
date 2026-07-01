package cn.stackssh.trigger.http;

import cn.stackssh.api.dto.ConversationRoundTraceItemDTO;
import cn.stackssh.api.dto.ConversationRoundTraceResponseDTO;
import cn.stackssh.api.dto.ConversationStateDiagnosticResponseDTO;
import cn.stackssh.api.response.Response;
import cn.stackssh.domain.agent.adapter.repository.IConversationStateRepository;
import cn.stackssh.domain.agent.adapter.repository.IConversationRoundTraceRepository;
import cn.stackssh.domain.agent.model.entity.ConversationRoundTraceEntity;
import cn.stackssh.domain.agent.model.entity.ConversationStateEntity;
import cn.stackssh.domain.agent.model.valobj.enhance.SearchContext;
import cn.stackssh.types.enums.ResponseCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat-session-state")
@CrossOrigin(origins = "*")
public class ConversationStateDiagnosticController {

    @Resource
    private IConversationStateRepository conversationStateRepository;

    @Resource
    private IConversationRoundTraceRepository conversationRoundTraceRepository;

    @Resource
    private ObjectMapper objectMapper;

    @GetMapping("/diagnostic")
    public Response<ConversationStateDiagnosticResponseDTO> queryDiagnostic(@RequestParam("sessionId") String sessionId) {
        try {
            ConversationStateEntity state = conversationStateRepository.findBySessionId(sessionId);
            if (state == null) {
                return Response.<ConversationStateDiagnosticResponseDTO>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info("Conversation state not found: " + sessionId)
                        .build();
            }

            ConversationStateDiagnosticResponseDTO dto = new ConversationStateDiagnosticResponseDTO();
            dto.setSessionId(state.getSessionId());
            dto.setUserId(state.getUserId());
            dto.setAgentId(state.getAgentId());
            dto.setCurrentIntent(state.getCurrentIntent());
            dto.setTaskSummary(state.getTaskSummary());
            dto.setIntentSummary(state.getIntentSummary());
            dto.setToolSummary(state.getToolSummary());
            dto.setTurnCount(state.getTurnCount());
            dto.setContextVersion(state.getContextVersion());
            dto.setLastRoundAt(state.getLastRoundAt());
            dto.setLastTerminalSessionId(state.getLastTerminalSessionId());
            dto.setLastUserMessageDigestPresent(state.getLastUserMessageDigest() != null && !state.getLastUserMessageDigest().isBlank());
            dto.setCachedSearchContextPresent(state.getCachedSearchContext() != null && !state.getCachedSearchContext().isBlank());
            dto.setCachedRagChunksPresent(state.getCachedRagChunks() != null && !state.getCachedRagChunks().isBlank());
            dto.setLastEnhancementCacheHit(state.getLastEnhancementCacheHit());
            dto.setLastEnhancementCacheReason(state.getLastEnhancementCacheReason());

            SearchContext searchContext = readSearchContext(state.getCachedSearchContext());
            dto.setCachedServiceStatusCount(searchContext.getServiceStatus().size());
            dto.setCachedFileContentCount(searchContext.getFileContents().size());
            dto.setCachedRecentLogCount(searchContext.getRecentLogs().size());
            dto.setCachedRagChunkCount(readRagChunks(state.getCachedRagChunks()).size());

            return Response.<ConversationStateDiagnosticResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(dto)
                    .build();
        } catch (Exception e) {
            log.error("Query conversation diagnostic failed sessionId={}", sessionId, e);
            return Response.<ConversationStateDiagnosticResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @GetMapping("/round-timeline")
    public Response<ConversationRoundTraceResponseDTO> queryRoundTimeline(@RequestParam("sessionId") String sessionId,
                                                                          @RequestParam(value = "limit", defaultValue = "10") int limit) {
        try {
            int safeLimit = Math.max(1, Math.min(limit, 50));
            List<ConversationRoundTraceEntity> traces = conversationRoundTraceRepository.queryRecentBySessionId(sessionId, safeLimit);
            ConversationRoundTraceResponseDTO dto = new ConversationRoundTraceResponseDTO();
            dto.setSessionId(sessionId);
            dto.setItems(traces.stream().map(this::toItem).toList());
            return Response.<ConversationRoundTraceResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(dto)
                    .build();
        } catch (Exception e) {
            log.error("Query conversation round timeline failed sessionId={}", sessionId, e);
            return Response.<ConversationRoundTraceResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    private SearchContext readSearchContext(String cachedSearchContext) {
        if (cachedSearchContext == null || cachedSearchContext.isBlank()) {
            return SearchContext.builder().build();
        }
        try {
            return objectMapper.readValue(cachedSearchContext, SearchContext.class);
        } catch (Exception e) {
            log.warn("Failed to parse cached search context: {}", e.getMessage());
            return SearchContext.builder().build();
        }
    }

    private List<String> readRagChunks(String cachedRagChunks) {
        if (cachedRagChunks == null || cachedRagChunks.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(cachedRagChunks, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse cached rag chunks: {}", e.getMessage());
            return List.of();
        }
    }

    private ConversationRoundTraceItemDTO toItem(ConversationRoundTraceEntity trace) {
        ConversationRoundTraceItemDTO item = new ConversationRoundTraceItemDTO();
        item.setTurnNumber(trace.getTurnNumber());
        item.setCurrentIntent(trace.getCurrentIntent());
        item.setEnhancementCacheHit(trace.getEnhancementCacheHit());
        item.setEnhancementCacheReason(trace.getEnhancementCacheReason());
        item.setRawUserMessage(trace.getRawUserMessage());
        item.setAssistantMessage(trace.getAssistantMessage());
        item.setToolSummary(trace.getToolSummary());
        item.setSuccess(trace.getSuccess());
        item.setErrorMessage(trace.getErrorMessage());
        item.setTotalSteps(trace.getTotalSteps());
        item.setCreatedAt(trace.getCreatedAt());
        return item;
    }
}
