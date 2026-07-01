package cn.stackssh.cases.react.node;

import cn.stackssh.api.dto.ChatRequestDTO;
import cn.stackssh.api.dto.ReActResultDTO;
import cn.stackssh.cases.react.AbstractAIAgentReActSupport;
import cn.stackssh.cases.react.factory.DefaultReActFactory;
import cn.stackssh.cases.react.service.IConversationContextAssembler;
import cn.stackssh.domain.agent.model.entity.RoundResultEntity;
import cn.stackssh.domain.agent.model.entity.ToolExecutionRecordEntity;
import cn.stackssh.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.stackssh.domain.agent.model.valobj.context.PromptEnvelopeVO;
import cn.stackssh.domain.agent.service.IConversationRoundService;
import cn.stackssh.domain.agent.service.armory.factory.DefaultArmoryFactory;
import cn.stackssh.domain.agent.service.armory.matter.mcp.server.SshExecuteMcpService;
import cn.stackssh.domain.agent.service.armory.matter.tools.SshExecuteAdkTool;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.events.EventActions;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component("reactAiCallNode")
public class AiCallNode extends AbstractAIAgentReActSupport {

    @Resource
    private DefaultArmoryFactory defaultArmoryFactory;

    @Resource
    private IConversationContextAssembler conversationContextAssembler;

    @Resource
    private IConversationRoundService conversationRoundService;

    private static final Map<String, String> STATE_DELTA_TOOL_MAPPING = Map.of(
            "ssh_result", "executeCommand"
    );

    @Override
    protected ReActResultDTO doApply(ChatRequestDTO requestParameter, DefaultReActFactory.DynamicContext dynamicContext) throws Exception {
        log.info("ReAct AiCallNode - start step {}", dynamicContext.getStep() + 1);

        String agentId = dynamicContext.getAgentId();
        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(agentId);
        if (aiAgentRegisterVO == null) {
            throw new RuntimeException("Agent not found: " + agentId);
        }

        Runner runner = aiAgentRegisterVO.getRunner();
        String lastUserMessage = getLastUserMessage(requestParameter, dynamicContext);
        dynamicContext.resetRoundBuffers();

        String terminalSessionId = dynamicContext.getTerminalSessionId();
        if (terminalSessionId != null && !terminalSessionId.isEmpty()) {
            SshExecuteAdkTool.setCurrentTerminalSession(terminalSessionId);
            SshExecuteMcpService.setCurrentTerminalSession(terminalSessionId);
            SshExecuteAdkTool.setTerminalSession(dynamicContext.getSessionId(), terminalSessionId);
        }

        PromptEnvelopeVO promptEnvelope = conversationContextAssembler.assemble(requestParameter, dynamicContext);
        dynamicContext.setCurrentIntent(
                promptEnvelope.getConversationState() != null
                        ? promptEnvelope.getConversationState().getCurrentIntent()
                        : null
        );
        dynamicContext.setMessageHistory(new ArrayList<>(promptEnvelope.getTrimmedHistory()));

        if (dynamicContext.getStep() == 0
                && requestParameter.getMessage() != null
                && !requestParameter.getMessage().isBlank()) {
            conversationRoundService.commitUserMessage(
                    dynamicContext.getSessionId(),
                    promptEnvelope.getRawUserMessage(),
                    promptEnvelope.getEstimatedTokens() != null
                            ? promptEnvelope.getEstimatedTokens()
                            : estimateTokens(lastUserMessage)
            );
        }

        Content userContent = Content.builder()
                .role("user")
                .parts(com.google.genai.types.Part.builder().text(promptEnvelope.getFinalUserMessage()).build())
                .build();

        dynamicContext.setStopReason(null);
        dynamicContext.setErrorMessage(null);

        ResponseBodyEmitter emitter = dynamicContext.getEmitter();
        StringBuilder textAccumulator = new StringBuilder();
        List<ToolExecutionRecordEntity> toolExecutionRecords = new ArrayList<>();
        Set<String> processedToolFingerprints = new HashSet<>();
        int roundToolCalls = 0;
        boolean hasError = false;
        StringBuilder errorBuilder = new StringBuilder();

        ensureAdkSession(runner, aiAgentRegisterVO.getAppName(), dynamicContext.getUserId(), dynamicContext.getSessionId());

        try {
            Iterator<Event> events = runner.runAsync(
                    dynamicContext.getUserId(),
                    dynamicContext.getSessionId(),
                    userContent,
                    RunConfig.builder().build()
            ).blockingIterable().iterator();

            while (events.hasNext()) {
                Event event = events.next();

                String eventText = event.stringifyContent();
                if (eventText != null && !eventText.isBlank()) {
                    textAccumulator.append(eventText);
                    dynamicContext.setAssistantContent(textAccumulator);
                    sendTextEvent(emitter, eventText, textAccumulator.toString());
                }

                EventActions actions = event.actions();
                if (actions == null) {
                    continue;
                }

                Map<String, Object> stateDelta = actions.stateDelta();
                if (stateDelta == null || stateDelta.isEmpty()) {
                    continue;
                }

                for (Map.Entry<String, Object> entry : stateDelta.entrySet()) {
                    String stateKey = entry.getKey();
                    Object stateValue = entry.getValue();
                    if ("REMOVED".equals(stateValue)) {
                        continue;
                    }

                    String toolName = resolveToolName(stateKey);
                    String resultContent = formatStateValue(stateValue);
                    String toolFingerprint = buildToolFingerprint(stateKey, resultContent);
                    if (!processedToolFingerprints.add(toolFingerprint)) {
                        log.debug("Skip duplicated tool result in same round: {}", toolFingerprint);
                        continue;
                    }
                    String toolCallId = "call_" + stateKey + "_" + System.currentTimeMillis();

                    Map<String, Object> toolCallInfo = new HashMap<>();
                    toolCallInfo.put("id", toolCallId);
                    toolCallInfo.put("name", toolName);
                    toolCallInfo.put("args", "");
                    dynamicContext.getCurrentToolCalls().add(toolCallInfo);

                    Map<String, Object> toolResultInfo = new HashMap<>();
                    toolResultInfo.put("id", toolCallId);
                    toolResultInfo.put("name", toolName);
                    toolResultInfo.put("content", resultContent);
                    toolResultInfo.put("status", "success");
                    dynamicContext.getCurrentToolResults().add(toolResultInfo);

                    sendToolCallEvent(emitter, toolCallId, toolName, "executing");
                    sendToolResultEvent(emitter, toolCallId, resultContent, "success");

                    roundToolCalls++;
                    dynamicContext.incrementTotalToolCalls();

                    if ("executeCommand".equals(toolName) && !resultContent.isEmpty()) {
                        recordExecutedCommand(dynamicContext, resultContent);
                    }

                    toolExecutionRecords.add(ToolExecutionRecordEntity.builder()
                            .toolCallId(toolCallId)
                            .toolName(toolName)
                            .resultContent(resultContent)
                            .status("success")
                            .estimatedTokens(estimateTokens(resultContent))
                            .build());

                    conversationRoundService.commitToolResult(
                            dynamicContext.getSessionId(),
                            toolName,
                            toolCallId,
                            resultContent,
                            "success",
                            estimateTokens(resultContent)
                    );
                }
            }
        } catch (Exception e) {
            log.error("ADK Runner call failed", e);
            hasError = true;
            errorBuilder.append("ADK Runner error: ").append(e.getMessage());
            dynamicContext.setErrorMessage(errorBuilder.toString());
            dynamicContext.setStopReason("error");
        } finally {
            if (terminalSessionId != null && !terminalSessionId.isEmpty()) {
                SshExecuteAdkTool.clearCurrentTerminalSession();
                SshExecuteMcpService.clearCurrentTerminalSession();
            }
        }

        if (textAccumulator.length() > 0) {
            dynamicContext.appendAssistantMessage(textAccumulator.toString());
            conversationRoundService.commitAssistantMessage(
                    dynamicContext.getSessionId(),
                    textAccumulator.toString(),
                    estimateTokens(textAccumulator.toString())
            );
        }

        dynamicContext.incrementStep();
        dynamicContext.getResult().setTotalSteps(dynamicContext.getStep());
        dynamicContext.getResult().setTotalToolCalls(
                dynamicContext.getResult().getTotalToolCalls() + roundToolCalls
        );

        sendRoundEndEvent(
                dynamicContext.getEmitter(),
                dynamicContext.getStep(),
                dynamicContext.getMaxSteps(),
                !hasError,
                dynamicContext.getResult().getTotalToolCalls()
        );

        if (hasError) {
            dynamicContext.setStopReason("error");
        }

        conversationRoundService.finishRound(RoundResultEntity.builder()
                .sessionId(dynamicContext.getSessionId())
                .userId(dynamicContext.getUserId())
                .agentId(dynamicContext.getAgentId())
                .terminalSessionId(dynamicContext.getTerminalSessionId())
                .rawUserMessage(promptEnvelope.getRawUserMessage())
                .assistantMessage(textAccumulator.toString())
                .ragChunks(promptEnvelope.getRagChunks())
                .currentIntent(dynamicContext.getCurrentIntent())
                .searchContext(promptEnvelope.getSearchContext())
                .enhancementCacheHit(promptEnvelope.getEnhancementCacheHit())
                .enhancementCacheReason(promptEnvelope.getEnhancementCacheReason())
                .toolExecutions(toolExecutionRecords)
                .success(!hasError)
                .errorMessage(hasError ? errorBuilder.toString() : null)
                .totalSteps(dynamicContext.getStep())
                .build());

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ChatRequestDTO, DefaultReActFactory.DynamicContext, ReActResultDTO> get(
            ChatRequestDTO requestParameter,
            DefaultReActFactory.DynamicContext dynamicContext) throws Exception {

        String stopReason = dynamicContext.getStopReason();
        if (stopReason != null) {
            return getBean("reactUserFeedbackNode");
        }

        if (dynamicContext.getStep() >= dynamicContext.getMaxSteps()) {
            dynamicContext.setStopReason("max_steps");
            return getBean("reactUserFeedbackNode");
        }

        if (!dynamicContext.getCurrentToolCalls().isEmpty()) {
            return getBean("reactToolCallNode");
        }

        return getBean("reactLoopDecisionNode");
    }

    private String getLastUserMessage(ChatRequestDTO requestParameter,
                                      DefaultReActFactory.DynamicContext dynamicContext) {
        if (requestParameter.getMessage() != null && !requestParameter.getMessage().isEmpty()) {
            return requestParameter.getMessage();
        }

        if (dynamicContext.getResolvedUserMessage() != null && !dynamicContext.getResolvedUserMessage().isBlank()) {
            return dynamicContext.getResolvedUserMessage();
        }

        List<Map<String, Object>> history = dynamicContext.getMessageHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            Map<String, Object> msg = history.get(i);
            if ("user".equals(msg.get("role"))) {
                return (String) msg.get("content");
            }
        }

        return "";
    }

    private String resolveToolName(String stateKey) {
        String mapped = STATE_DELTA_TOOL_MAPPING.get(stateKey);
        if (mapped != null) {
            return mapped;
        }
        if (stateKey.endsWith("_result")) {
            return stateKey.substring(0, stateKey.length() - 7);
        }
        return stateKey;
    }

    private String formatStateValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return (String) value;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }

    private void recordExecutedCommand(DefaultReActFactory.DynamicContext dynamicContext, String toolResult) {
        if (toolResult.length() > 1000) {
            dynamicContext.addRecentCommand(truncate(toolResult, 80) + "...");
        } else {
            dynamicContext.addRecentCommand(toolResult);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    private int estimateTokens(String content) {
        return content == null ? 0 : content.length() / 2;
    }

    private String buildToolFingerprint(String stateKey, String resultContent) {
        return stateKey + "::" + (resultContent == null ? "" : resultContent);
    }

    private void ensureAdkSession(Runner runner, String appName, String userId, String sessionId) {
        try {
            Session existing = runner.sessionService()
                    .getSession(appName, userId, sessionId, null)
                    .blockingGet();
            if (existing == null) {
                createAdkSession(runner, appName, userId, sessionId);
            }
        } catch (Exception e) {
            log.debug("ADK getSession failed, creating session instead: {}", e.getMessage());
            createAdkSession(runner, appName, userId, sessionId);
        }
    }

    private void createAdkSession(Runner runner, String appName, String userId, String sessionId) {
        try {
            runner.sessionService()
                    .createSession(appName, userId, new HashMap<>(), sessionId)
                    .blockingGet();
            log.info("ADK session created: appName={} userId={} sessionId={}", appName, userId, sessionId);
        } catch (Exception ex) {
            log.warn("ADK session create skipped: {}", ex.getMessage());
        }
    }

}
