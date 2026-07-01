package cn.stackssh.cases.react.service;

import cn.stackssh.api.dto.ChatRequestDTO;
import cn.stackssh.cases.react.factory.DefaultReActFactory;
import cn.stackssh.domain.agent.adapter.repository.IChatHistoryRepository;
import cn.stackssh.domain.agent.adapter.repository.IConversationStateRepository;
import cn.stackssh.domain.agent.model.entity.ChatMessageEntity;
import cn.stackssh.domain.agent.model.entity.ConversationStateEntity;
import cn.stackssh.domain.agent.model.valobj.context.ContextBudgetVO;
import cn.stackssh.domain.agent.model.valobj.context.PromptEnvelopeVO;
import cn.stackssh.domain.agent.model.valobj.enhance.SearchContext;
import cn.stackssh.domain.agent.model.valobj.intent.IntentResultVO;
import cn.stackssh.domain.agent.model.valobj.prompt.PromptContextVO;
import cn.stackssh.domain.agent.service.IChatContextService;
import cn.stackssh.domain.agent.service.IContextBudgetService;
import cn.stackssh.domain.agent.service.IIntentEnhancerService;
import cn.stackssh.domain.agent.service.IIntentService;
import cn.stackssh.domain.agent.service.IPromptService;
import cn.stackssh.domain.knowledge.service.IKnowledgeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ConversationContextAssembler implements IConversationContextAssembler {

    @Resource
    private IConversationStateRepository conversationStateRepository;

    @Resource
    private IIntentService intentService;

    @Resource
    private IIntentEnhancerService intentEnhancerService;

    @Resource
    private IKnowledgeService knowledgeService;

    @Resource
    private IContextBudgetService contextBudgetService;

    @Resource
    private IChatHistoryRepository chatHistoryRepository;

    @Resource
    private IChatContextService chatContextService;

    @Resource
    private IPromptService promptService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public PromptEnvelopeVO assemble(ChatRequestDTO requestDTO, DefaultReActFactory.DynamicContext dynamicContext) {
        String sessionId = dynamicContext.getSessionId();
        String userMessage = resolveUserMessage(requestDTO, dynamicContext);

        ConversationStateEntity state = conversationStateRepository.findBySessionId(sessionId);
        if (state == null) {
            state = ConversationStateEntity.builder()
                    .sessionId(sessionId)
                    .userId(dynamicContext.getUserId())
                    .agentId(dynamicContext.getAgentId())
                    .turnCount(0)
                    .contextVersion(1)
                    .build();
        }

        String previousIntent = state.getCurrentIntent();
        IntentResultVO intentResult = classifyIntent(sessionId, dynamicContext.getUserId(), userMessage);
        String currentIntent = intentResult != null && intentResult.getIntent() != null
                ? intentResult.getIntent().name()
                : previousIntent;
        state.setCurrentIntent(currentIntent);

        CacheDecision cacheDecision = evaluateEnhancementReuse(
                state,
                previousIntent,
                currentIntent,
                userMessage,
                dynamicContext.getTerminalSessionId()
        );
        log.info("Conversation enhancement cache {}: sessionId={}, reason={}, previousIntent={}, currentIntent={}, terminalSessionId={}",
                cacheDecision.reuse ? "hit" : "miss",
                sessionId,
                cacheDecision.reason,
                previousIntent,
                currentIntent,
                dynamicContext.getTerminalSessionId());
        state.setLastEnhancementCacheHit(cacheDecision.reuse);
        state.setLastEnhancementCacheReason(cacheDecision.reason);
        SearchContext searchContext = cacheDecision.reuse
                ? readCachedSearchContext(state)
                : runEnhancement(dynamicContext.getTerminalSessionId(), userMessage);
        List<String> ragChunks = cacheDecision.reuse
                ? readCachedRagChunks(state)
                : runRagSearch(userMessage, requestDTO.getConnectionId());

        ContextBudgetVO budget = contextBudgetService.allocate(userMessage, state, searchContext, ragChunks, 12000);
        List<Map<String, Object>> history = loadHistory(sessionId, budget.getHistoryBudget(), dynamicContext);

        PromptContextVO promptContextVO = chatContextService.buildPromptContext(
                sessionId,
                dynamicContext.getUserId(),
                dynamicContext.getTerminalSessionId(),
                history,
                state,
                searchContext,
                ragChunks
        );
        promptContextVO.setRecentCommands(dynamicContext.getRecentCommands());

        PromptEnvelopeVO envelope = PromptEnvelopeVO.builder()
                .sessionId(sessionId)
                .userId(dynamicContext.getUserId())
                .terminalSessionId(dynamicContext.getTerminalSessionId())
                .rawUserMessage(userMessage)
                .conversationState(state)
                .searchContext(searchContext)
                .ragChunks(ragChunks)
                .trimmedHistory(history)
                .budget(budget)
                .enhancementCacheHit(cacheDecision.reuse)
                .enhancementCacheReason(cacheDecision.reason)
                .estimatedTokens(userMessage != null ? userMessage.length() / 2 : 0)
                .build();
        envelope.setFinalUserMessage(promptService.buildFinalUserMessage(envelope, promptContextVO));
        return envelope;
    }

    private String resolveUserMessage(ChatRequestDTO requestDTO, DefaultReActFactory.DynamicContext dynamicContext) {
        if (requestDTO.getMessage() != null && !requestDTO.getMessage().isBlank()) {
            dynamicContext.setResolvedUserMessage(requestDTO.getMessage());
            return requestDTO.getMessage();
        }
        if (dynamicContext.getResolvedUserMessage() != null && !dynamicContext.getResolvedUserMessage().isBlank()) {
            return dynamicContext.getResolvedUserMessage();
        }
        List<Map<String, Object>> history = dynamicContext.getMessageHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            Map<String, Object> message = history.get(i);
            if ("user".equals(message.get("role"))) {
                Object content = message.get("content");
                if (content != null && !content.toString().isBlank()) {
                    String resolved = content.toString();
                    dynamicContext.setResolvedUserMessage(resolved);
                    return resolved;
                }
            }
        }
        dynamicContext.setResolvedUserMessage("");
        return "";
    }

    private IntentResultVO classifyIntent(String sessionId, String userId, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return null;
        }
        return intentService.classify(sessionId, userId, userMessage);
    }

    private CacheDecision evaluateEnhancementReuse(ConversationStateEntity state, String previousIntent, String currentIntent,
                                                   String userMessage, String terminalSessionId) {
        String currentDigest = digestMessage(userMessage);
        if (state.getTurnCount() == null || state.getTurnCount() <= 0) {
            return CacheDecision.miss("no_previous_turn");
        }
        if (state.getCachedSearchContext() == null || state.getCachedSearchContext().isBlank()) {
            return CacheDecision.miss("missing_search_context");
        }
        if (state.getCachedRagChunks() == null || state.getCachedRagChunks().isBlank()) {
            return CacheDecision.miss("missing_rag_chunks");
        }
        if (state.getLastUserMessageDigest() == null) {
            return CacheDecision.miss("missing_message_digest");
        }
        if (!state.getLastUserMessageDigest().equals(currentDigest)) {
            return CacheDecision.miss("message_digest_changed");
        }
        if (!equalsNullable(state.getLastTerminalSessionId(), terminalSessionId)) {
            return CacheDecision.miss("terminal_session_changed");
        }
        if (previousIntent == null || !previousIntent.equals(currentIntent)) {
            return CacheDecision.miss("intent_changed");
        }
        return CacheDecision.hit("matched_previous_round");
    }

    private SearchContext runEnhancement(String terminalSessionId, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return SearchContext.builder().build();
        }
        SearchContext searchContext = intentEnhancerService.enhance(terminalSessionId, userMessage);
        return searchContext != null ? searchContext : SearchContext.builder().build();
    }

    private List<String> runRagSearch(String userMessage, String connectionId) {
        if (userMessage == null || userMessage.isBlank()) {
            return List.of();
        }
        List<String> ragChunks = knowledgeService.searchRelevant(userMessage, connectionId, 3);
        return ragChunks != null ? ragChunks : List.of();
    }

    private List<Map<String, Object>> loadHistory(String sessionId, int historyBudget, DefaultReActFactory.DynamicContext dynamicContext) {
        if (dynamicContext.isHistoryLoadedFromRepository()) {
            return dynamicContext.getMessageHistory();
        }
        List<ChatMessageEntity> historyEntities = chatHistoryRepository.getMessagesWithBudget(sessionId, historyBudget);
        return historyEntities.stream().map(this::toMap).toList();
    }

    private SearchContext readCachedSearchContext(ConversationStateEntity state) {
        try {
            return objectMapper.readValue(state.getCachedSearchContext(), SearchContext.class);
        } catch (Exception e) {
            return SearchContext.builder().build();
        }
    }

    private List<String> readCachedRagChunks(ConversationStateEntity state) {
        try {
            return objectMapper.readValue(state.getCachedRagChunks(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> toMap(ChatMessageEntity msg) {
        Map<String, Object> map = new HashMap<>();
        map.put("role", msg.getRole());
        map.put("content", msg.getContent() != null ? msg.getContent() : "");
        if ("tool".equals(msg.getRole()) && msg.getToolCallId() != null) {
            map.put("tool_call_id", msg.getToolCallId());
            map.put("name", msg.getToolName());
        }
        return map;
    }

    public static String digestMessage(String rawUserMessage) {
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

    private boolean equalsNullable(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private static class CacheDecision {
        private final boolean reuse;
        private final String reason;

        private CacheDecision(boolean reuse, String reason) {
            this.reuse = reuse;
            this.reason = reason;
        }

        private static CacheDecision hit(String reason) {
            return new CacheDecision(true, reason);
        }

        private static CacheDecision miss(String reason) {
            return new CacheDecision(false, reason);
        }
    }
}
