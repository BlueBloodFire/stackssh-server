package cn.stackssh.domain.agent.service.context;

import cn.stackssh.domain.agent.model.entity.ConversationStateEntity;
import cn.stackssh.domain.agent.model.valobj.enhance.SearchContext;
import cn.stackssh.domain.agent.model.valobj.prompt.MilestoneVO;
import cn.stackssh.domain.agent.model.valobj.prompt.PromptContextVO;
import cn.stackssh.domain.agent.service.IChatContextService;
import cn.stackssh.domain.agent.service.context.provider.ContextProvider;
import cn.stackssh.domain.agent.service.context.reducer.HybridReducer;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.stackssh.domain.agent.service.context.provider.ToolResultProvider;

@Service
public class ChatContextService implements IChatContextService {
    private static final int DEFAULT_MAX_CONTEXT_TOKENS = 8000;

    @Resource
    private List<ContextProvider> providers;
    
    @Resource
    private HybridReducer hybridReducer;
    
    @Resource
    private ToolResultProvider toolResultProvider;

    @PostConstruct
    public void init() {
        providers.sort(Comparator.comparingInt(ContextProvider::getOrder));
    }

    @Override
    @SuppressWarnings("unchecked")
    public PromptContextVO buildPromptContext(String sessionId, String userId, String terminalSessionId, List<Map<String, Object>> messageHistory) {
        return buildPromptContext(sessionId, userId, terminalSessionId, messageHistory, null, null, List.of());
    }

    @Override
    @SuppressWarnings("unchecked")
    public PromptContextVO buildPromptContext(String sessionId, String userId, String terminalSessionId,
                                              List<Map<String, Object>> messageHistory, ConversationStateEntity conversationState,
                                              SearchContext searchContext, List<String> ragChunks) {
        Map<String, Object> finalCtx = new HashMap<>();

        for (ContextProvider provider : providers) {
            if (!provider.enabled()) continue;
            Map<String, Object> ctx = provider.provide(sessionId, userId, terminalSessionId, messageHistory);
            if (ctx != null) {
                finalCtx.putAll(ctx);
            }
        }

        PromptContextVO promptContextVO = PromptContextVO.builder()
                .osInfo((String) finalCtx.get("osInfo"))
                .currentUser((String) finalCtx.get("currentUser"))
                .currentDirectory((String) finalCtx.get("currentDirectory"))
                .serverInfo((String) finalCtx.get("serverInfo"))
                .milestoneVOS((List<MilestoneVO>) finalCtx.get("milestoneVOS"))
                .toolResultSummary((String) finalCtx.get("toolResultSummary"))
                .taskDescription((String) finalCtx.get("taskDescription"))
                .build();

        if (conversationState != null) {
            if (conversationState.getTaskSummary() != null && !conversationState.getTaskSummary().isBlank()) {
                promptContextVO.setTaskDescription(conversationState.getTaskSummary());
            }
            if (conversationState.getToolSummary() != null && !conversationState.getToolSummary().isBlank()) {
                promptContextVO.setToolResultSummary(conversationState.getToolSummary());
            }
        }

        if (searchContext != null && !searchContext.isEmpty()) {
            promptContextVO.setSearchContext(searchContext);
        }

        return promptContextVO;
    }

    @Override
    public List<Map<String, Object>> trimHistory(List<Map<String, Object>> history, int tokenBudget) {
        if (history == null || history.isEmpty()) return Collections.emptyList();
        return hybridReducer.reduce(history, tokenBudget > 0 ? tokenBudget : DEFAULT_MAX_CONTEXT_TOKENS);
    }
    
    @Override
    public void pushToolResult(String sessionId, String toolName, String result) {
        toolResultProvider.pushResult(sessionId, toolName, result);
    }
}
