package cn.stackssh.cases.react.node;

import cn.stackssh.api.dto.ChatRequestDTO;
import cn.stackssh.api.dto.ReActResultDTO;
import cn.stackssh.cases.react.AbstractAIAgentReActSupport;
import cn.stackssh.cases.react.factory.DefaultReActFactory;
import cn.stackssh.domain.agent.adapter.repository.IChatHistoryRepository;
import cn.stackssh.domain.agent.adapter.repository.IConversationStateRepository;
import cn.stackssh.domain.agent.model.entity.ChatMessageEntity;
import cn.stackssh.domain.agent.model.entity.ConversationStateEntity;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component("reactRootNode")
public class RootNode extends AbstractAIAgentReActSupport {

    @Resource
    private IChatHistoryRepository chatHistoryRepository;

    @Resource
    private IConversationStateRepository conversationStateRepository;

    private static final int DEFAULT_MAX_STEPS = 50;
    private static final int DEFAULT_MAX_TOOL_CALLS = 200;
    private static final int DEFAULT_MAX_TOOL_CALLS_PER_ROUND = 10;

    @Override
    protected ReActResultDTO doApply(ChatRequestDTO requestParameter, DefaultReActFactory.DynamicContext dynamicContext) throws Exception {
        String sessionId = requestParameter.getSessionId();
        String userId = requestParameter.getUserId();
        String agentId = requestParameter.getAgentId();
        String terminalSessionId = requestParameter.getTerminalSessionId();
        String resolvedTerminalSessionId = terminalSessionId;

        if (resolvedTerminalSessionId != null && !resolvedTerminalSessionId.isEmpty()) {
            setCurrentTerminalSession(resolvedTerminalSessionId);
        } else {
            String boundTerminal = getTerminalSession(sessionId);
            if (boundTerminal != null && !boundTerminal.isEmpty()) {
                setCurrentTerminalSession(boundTerminal);
                resolvedTerminalSessionId = boundTerminal;
            }
        }

        dynamicContext.setSessionId(sessionId);
        dynamicContext.setUserId(userId);
        dynamicContext.setAgentId(agentId);
        dynamicContext.setTerminalSessionId(resolvedTerminalSessionId);
        dynamicContext.setCurrentToolCalls(new ArrayList<>());
        dynamicContext.setCurrentToolResults(new ArrayList<>());
        dynamicContext.setMessageHistory(new ArrayList<>());
        dynamicContext.setHistoryLoadedFromRepository(false);

        ConversationStateEntity conversationState = conversationStateRepository.findBySessionId(sessionId);
        if (conversationState == null) {
            conversationStateRepository.upsert(ConversationStateEntity.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .agentId(agentId)
                    .lastTerminalSessionId(resolvedTerminalSessionId)
                    .turnCount(0)
                    .contextVersion(1)
                    .build());
        } else if (resolvedTerminalSessionId != null && !resolvedTerminalSessionId.isEmpty()) {
            conversationStateRepository.updateTerminalBinding(sessionId, resolvedTerminalSessionId);
        }

        String resolvedUserMessage = requestParameter.getMessage();
        if (resolvedUserMessage == null || resolvedUserMessage.trim().isEmpty()) {
            List<ChatMessageEntity> recentMessages = chatHistoryRepository.getRecentMessages(sessionId, 20);
            dynamicContext.setMessageHistory(toHistory(recentMessages));
            dynamicContext.setHistoryLoadedFromRepository(true);
            resolvedUserMessage = findLastUserMessage(recentMessages);
        }
        dynamicContext.setResolvedUserMessage(resolvedUserMessage);

        dynamicContext.setCurrentStep(new AtomicInteger(0));
        dynamicContext.setMaxSteps(DEFAULT_MAX_STEPS);
        dynamicContext.setMaxToolCalls(DEFAULT_MAX_TOOL_CALLS);
        dynamicContext.setMaxToolCallsPerRound(DEFAULT_MAX_TOOL_CALLS_PER_ROUND);

        ReActResultDTO result = ReActResultDTO.builder()
                .totalSteps(0)
                .totalToolCalls(0)
                .maxStepsReached(false)
                .userStopped(false)
                .idleTimeout(false)
                .build();
        dynamicContext.setResult(result);

        log.info("ReAct RootNode initialized: sessionId={}, userId={}, agentId={}, terminalSessionId={}, historyPreloaded={}",
                sessionId, userId, agentId, resolvedTerminalSessionId, dynamicContext.isHistoryLoadedFromRepository());
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ChatRequestDTO, DefaultReActFactory.DynamicContext, ReActResultDTO> get(
            ChatRequestDTO requestParameter,
            DefaultReActFactory.DynamicContext dynamicContext) throws Exception {
        return getBean("reactAiCallNode");
    }

    private List<Map<String, Object>> toHistory(List<ChatMessageEntity> recentMessages) {
        List<Map<String, Object>> history = new ArrayList<>();
        for (ChatMessageEntity msg : recentMessages) {
            Map<String, Object> map = new HashMap<>();
            map.put("role", msg.getRole());
            map.put("content", msg.getContent() != null ? msg.getContent() : "");
            if ("tool".equals(msg.getRole()) && msg.getToolCallId() != null) {
                map.put("tool_call_id", msg.getToolCallId());
                map.put("name", msg.getToolName());
            }
            history.add(map);
        }
        return history;
    }

    private String findLastUserMessage(List<ChatMessageEntity> recentMessages) {
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            ChatMessageEntity message = recentMessages.get(i);
            if ("user".equals(message.getRole()) && message.getContent() != null && !message.getContent().isBlank()) {
                return message.getContent();
            }
        }
        return "";
    }

}
