package cn.stackssh.domain.agent.service.chat;

import cn.stackssh.domain.agent.adapter.repository.IChatHistoryRepository;
import cn.stackssh.domain.agent.adapter.repository.IConversationRoundTraceRepository;
import cn.stackssh.domain.agent.adapter.repository.IConversationStateRepository;
import cn.stackssh.domain.agent.model.entity.ChatMessageEntity;
import cn.stackssh.domain.agent.model.entity.ConversationRoundTraceEntity;
import cn.stackssh.domain.agent.model.entity.ConversationStateEntity;
import cn.stackssh.domain.agent.model.entity.RoundResultEntity;
import cn.stackssh.domain.agent.service.IConversationRoundService;
import cn.stackssh.domain.agent.service.IPromptService;
import cn.stackssh.domain.agent.service.IToolSummaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ConversationRoundService implements IConversationRoundService {

    private static final int MAX_TASK_SUMMARY_LENGTH = 500;
    private static final int MAX_INTENT_SUMMARY_LENGTH = 500;
    private static final int MAX_INTENT_HISTORY_ITEMS = 8;

    @Resource
    private IChatHistoryRepository chatHistoryRepository;

    @Resource
    private IConversationStateRepository conversationStateRepository;

    @Resource
    private IConversationRoundTraceRepository conversationRoundTraceRepository;

    @Resource
    private IToolSummaryService toolSummaryService;

    @Resource
    private IPromptService promptService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void commitUserMessage(String sessionId, String rawUserMessage, int estimatedTokens) {
        chatHistoryRepository.saveMessage(ChatMessageEntity.builder()
                .sessionId(sessionId)
                .role("user")
                .content(rawUserMessage)
                .priority("MEDIUM")
                .tokenCount(estimatedTokens)
                .build());
        promptService.detectAndRecordMilestone(sessionId, "user", rawUserMessage);
    }

    @Override
    public void commitAssistantMessage(String sessionId, String assistantMessage, int estimatedTokens) {
        chatHistoryRepository.saveMessage(ChatMessageEntity.builder()
                .sessionId(sessionId)
                .role("assistant")
                .content(assistantMessage)
                .priority("MEDIUM")
                .tokenCount(estimatedTokens)
                .build());
    }

    @Override
    public void commitToolResult(String sessionId, String toolName, String toolCallId, String resultContent, String status, int estimatedTokens) {
        chatHistoryRepository.saveMessage(ChatMessageEntity.builder()
                .sessionId(sessionId)
                .role("tool")
                .content(resultContent)
                .toolName(toolName)
                .toolCallId(toolCallId)
                .priority("MEDIUM")
                .tokenCount(estimatedTokens)
                .build());
        promptService.detectAndRecordMilestone(sessionId, "tool", resultContent);
        toolSummaryService.appendResult(sessionId, toolName, resultContent);
    }

    @Override
    public void finishRound(RoundResultEntity roundResult) {
        ConversationStateEntity state = conversationStateRepository.findBySessionId(roundResult.getSessionId());
        if (state == null) {
            state = ConversationStateEntity.builder()
                    .sessionId(roundResult.getSessionId())
                    .userId(roundResult.getUserId())
                    .agentId(roundResult.getAgentId())
                    .contextVersion(1)
                    .turnCount(0)
                    .build();
        }

        int nextTurn = state.getTurnCount() == null ? 1 : state.getTurnCount() + 1;
        state.setTurnCount(nextTurn);
        state.setCurrentIntent(roundResult.getCurrentIntent());
        state.setLastTerminalSessionId(roundResult.getTerminalSessionId());
        state.setLastRoundAt(System.currentTimeMillis());
        state.setToolSummary(toolSummaryService.buildSummary(roundResult.getSessionId()));
        state.setIntentSummary(mergeIntentSummary(state.getIntentSummary(), roundResult.getCurrentIntent()));

        if ((state.getTaskSummary() == null || state.getTaskSummary().isBlank())
                && roundResult.getRawUserMessage() != null && !roundResult.getRawUserMessage().isBlank()) {
            state.setTaskSummary(truncate(roundResult.getRawUserMessage(), MAX_TASK_SUMMARY_LENGTH));
        }

        state.setLastUserMessageDigest(digestMessage(roundResult.getRawUserMessage()));
        state.setCachedSearchContext(writeJson(roundResult.getSearchContext()));
        state.setCachedRagChunks(writeJson(roundResult.getRagChunks() != null ? roundResult.getRagChunks() : List.of()));
        state.setLastEnhancementCacheHit(roundResult.getEnhancementCacheHit());
        state.setLastEnhancementCacheReason(roundResult.getEnhancementCacheReason());

        conversationStateRepository.upsert(state);
        conversationRoundTraceRepository.save(ConversationRoundTraceEntity.builder()
                .sessionId(roundResult.getSessionId())
                .userId(roundResult.getUserId())
                .agentId(roundResult.getAgentId())
                .turnNumber(nextTurn)
                .currentIntent(roundResult.getCurrentIntent())
                .enhancementCacheHit(roundResult.getEnhancementCacheHit())
                .enhancementCacheReason(roundResult.getEnhancementCacheReason())
                .rawUserMessage(roundResult.getRawUserMessage())
                .assistantMessage(roundResult.getAssistantMessage())
                .toolSummary(state.getToolSummary())
                .success(roundResult.isSuccess())
                .errorMessage(roundResult.getErrorMessage())
                .totalSteps(roundResult.getTotalSteps())
                .createdAt(state.getLastRoundAt())
                .build());
    }

    private String mergeIntentSummary(String existingSummary, String currentIntent) {
        if (currentIntent == null || currentIntent.isBlank()) {
            return truncate(existingSummary, MAX_INTENT_SUMMARY_LENGTH);
        }

        LinkedHashSet<String> items = new LinkedHashSet<>();
        items.add(currentIntent);

        if (existingSummary != null && !existingSummary.isBlank()) {
            String[] parts = existingSummary.split("\\s*\\|\\s*");
            for (String part : parts) {
                if (part != null && !part.isBlank()) {
                    items.add(part.trim());
                }
            }
        }

        List<String> compacted = new ArrayList<>(items);
        if (compacted.size() > MAX_INTENT_HISTORY_ITEMS) {
            compacted = compacted.subList(0, MAX_INTENT_HISTORY_ITEMS);
        }

        return truncate(String.join(" | ", compacted), MAX_INTENT_SUMMARY_LENGTH);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String digestMessage(String rawUserMessage) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = messageDigest.digest((rawUserMessage == null ? "" : rawUserMessage).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(String.format("%02x", aByte));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString((rawUserMessage == null ? "" : rawUserMessage).hashCode());
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
