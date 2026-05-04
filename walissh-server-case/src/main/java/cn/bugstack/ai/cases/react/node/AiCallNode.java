package cn.bugstack.ai.cases.react.node;

import cn.bugstack.ai.api.dto.ChatRequestDTO;
import cn.bugstack.ai.api.dto.ReActResultDTO;
import cn.bugstack.ai.cases.react.AbstractAIAgentReActSupport;
import cn.bugstack.ai.cases.react.factory.DefaultReActFactory;
import cn.bugstack.ai.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.bugstack.ai.domain.agent.service.armory.factory.DefaultArmoryFactory;
import cn.bugstack.ai.domain.agent.service.armory.matter.mcp.server.SshExecuteMcpService;
import cn.bugstack.ai.domain.agent.service.armory.matter.tools.SshExecuteAdkTool;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 调用节点（ReAct 循环核心）
 *
 * <p>职责：
 * 1. 获取装配好的 Agent Runner
 * 2. 构建用户消息 Content
 * 3. 调用 runner.runAsync() 获取流式事件
 * 4. 处理 ADK 事件流（文本、工具调用、工具响应、reasoning）
 * 5. 累积响应文本和工具调用信息
 * 6. 发送结构化 SSE 事件到前端
 * 7. 路由到 LoopDecisionNode（循环判断）或 UserFeedbackNode（完成）
 *
 * <p>ADK 事件流特征（单次 runAsync 包含完整 ReAct 循环）：
 * <pre>
 * Event 1: [text]          → 模型思考/文本输出
 * Event 2: [function_call] → 模型决定调用工具（ADK 自动执行）
 * Event 3: [text]          → 模型基于工具结果的回复
 * ...
 * Event N: [final_response] → 无更多工具调用，对话结束
 * </pre>
 *
 * <p>ReAct 循环流程：
 * <pre>
 * RootNode
 *   └→ [本次] AiCallNode（调用 ADK Runner，处理事件流）
 *         └→ LoopDecisionNode → [终止] UserFeedbackNode
 * </pre>
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2026/5/4
 */
@Slf4j
@Component("reactAiCallNode")
public class AiCallNode extends AbstractAIAgentReActSupport {

    @Resource
    private DefaultArmoryFactory defaultArmoryFactory;

    /** SSE 事件发送间隔（字符数），避免过于频繁 */
    private static final int SSE_BATCH_SIZE = 20;

    @Override
    protected ReActResultDTO doApply(ChatRequestDTO requestParameter, DefaultReActFactory.DynamicContext dynamicContext) throws Exception {
        log.info("ReAct AiCallNode - 开始 AI 调用，第 {} 步", dynamicContext.getStep() + 1);

        String agentId = dynamicContext.getAgentId();
        String sessionId = dynamicContext.getSessionId();
        String userId = dynamicContext.getUserId();
        String terminalSessionId = dynamicContext.getTerminalSessionId();

        // 1. 获取 Agent Runner
        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(agentId);
        if (aiAgentRegisterVO == null) {
            throw new RuntimeException("Agent not found: " + agentId);
        }

        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        // 2. 获取最新用户消息（第一轮从 requestParameter，后续轮从 messageHistory）
        String lastUserMessage = getLastUserMessage(requestParameter, dynamicContext);
        Content userMsg = Content.fromParts(Part.fromText(lastUserMessage));

        // 3. 重置当前轮次缓冲
        dynamicContext.resetRoundBuffers();

        // 4. 绑定终端会话 ID 到 ThreadLocal（供 MCP/SSH 工具使用）
        if (terminalSessionId != null && !terminalSessionId.isEmpty()) {
            SshExecuteAdkTool.setCurrentTerminalSession(terminalSessionId);
            SshExecuteMcpService.setCurrentTerminalSession(terminalSessionId);
        }

        // 5. 流式调用 ADK Runner
        AtomicBoolean hasError = new AtomicBoolean(false);
        StringBuilder errorBuilder = new StringBuilder();
        StringBuilder textAccumulator = new StringBuilder();
        AtomicInteger sseCounter = new AtomicInteger(0);
        AtomicInteger roundToolCalls = new AtomicInteger(0);

        Flowable<Event> events = runner.runAsync(userId, sessionId, userMsg);

        events.blockingForEach(event -> {
            try {
                processEvent(event, dynamicContext, textAccumulator, sseCounter, roundToolCalls, hasError, errorBuilder);
            } catch (Exception e) {
                log.error("处理 ADK 事件异常", e);
                hasError.set(true);
                errorBuilder.append("Event processing error: ").append(e.getMessage());
            }
        });

        // 6. 发送剩余的文本缓冲
        flushTextBuffer(dynamicContext.getEmitter(), textAccumulator);

        // 7. 保存累积的响应到上下文
        String fullResponse = textAccumulator.toString();
        if (fullResponse != null && !fullResponse.isBlank()) {
            dynamicContext.setAssistantContent(new StringBuilder(fullResponse));
            dynamicContext.appendAssistantMessage(fullResponse);
        }

        // 8. 更新步数和工具调用统计
        dynamicContext.incrementStep();
        dynamicContext.getResult().setTotalSteps(dynamicContext.getStep());
        dynamicContext.getResult().setTotalToolCalls(
                dynamicContext.getResult().getTotalToolCalls() + roundToolCalls.get()
        );

        log.info("ReAct AiCallNode - 第 {} 步完成，本轮工具调用 {} 次，文本长度 {}",
                dynamicContext.getStep(), roundToolCalls.get(), fullResponse.length());

        // 9. 发送本轮结束事件
        sendRoundEndEvent(
                dynamicContext.getEmitter(),
                dynamicContext.getStep(),
                dynamicContext.getMaxSteps(),
                !hasError.get(),
                dynamicContext.getResult().getTotalToolCalls()
        );

        // 10. 错误处理
        if (hasError.get()) {
            dynamicContext.setErrorMessage(errorBuilder.toString());
            dynamicContext.setStopReason("error");
        }

        // 11. 路由到循环判断节点
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ChatRequestDTO, DefaultReActFactory.DynamicContext, ReActResultDTO> get(
            ChatRequestDTO requestParameter,
            DefaultReActFactory.DynamicContext dynamicContext) throws Exception {

        // 检查是否应该终止
        String stopReason = dynamicContext.getStopReason();
        if (stopReason != null) {
            log.info("检测到终止条件: {}, 路由到 UserFeedbackNode", stopReason);
            return getBean("reactUserFeedbackNode");
        }

        // 检查是否达到最大步数
        if (dynamicContext.getStep() >= dynamicContext.getMaxSteps()) {
            log.info("达到最大步数 {}, 路由到 UserFeedbackNode", dynamicContext.getMaxSteps());
            dynamicContext.setStopReason("max_steps");
            return getBean("reactUserFeedbackNode");
        }

        // ADK 处理完整 ReAct 循环，单次调用已包含工具执行
        // 这里可以扩展：检测是否需要多轮对话（如用户确认后继续）
        // 当前版本：单次 ADK 调用 = 完整 ReAct 循环
        log.info("ReAct 循环完成，路由到 UserFeedbackNode");
        return getBean("reactUserFeedbackNode");
    }

    // ═══════════════════════════════════════════════════════════════
    //  ADK 事件处理（核心）
    // ═══════════════════════════════════════════════════════════════

    /**
     * 处理单个 ADK Event
     *
     * <p>ADK 事件类型：
     * - 文本片段：event.content() → Part.text()
     * - 函数调用：event.functionCalls() → FunctionCall（ADK 自动执行）
     * - 函数响应：event.functionResponses() → FunctionResponse
     * - 最终响应：event.finalResponse() → true
     */
    private void processEvent(
            Event event,
            DefaultReActFactory.DynamicContext dynamicContext,
            StringBuilder textAccumulator,
            AtomicInteger sseCounter,
            AtomicInteger roundToolCalls,
            AtomicBoolean hasError,
            StringBuilder errorBuilder) {

        ResponseBodyEmitter emitter = dynamicContext.getEmitter();

        // 1. 处理文本内容
        List<String> textParts = extractTextParts(event);
        for (String text : textParts) {
            textAccumulator.append(text);
            sseCounter.addAndGet(text.length());

            // 按批次发送 SSE（减少网络压力）
            if (sseCounter.get() >= SSE_BATCH_SIZE) {
                sendTextEvent(emitter, textAccumulator.substring(
                                Math.max(0, textAccumulator.length() - sseCounter.get())),
                        textAccumulator.toString());
                sseCounter.set(0);
            }
        }

        // 2. 处理工具调用（FunctionCall）
        List<FunctionCall> functionCalls = event.functionCalls();
        if (!functionCalls.isEmpty()) {
            for (FunctionCall fc : functionCalls) {
                String toolCallId = fc.id().orElse("call_" + System.currentTimeMillis());
                String toolName = fc.name().orElse("unknown");
                String args = fc.args().map(a -> a.toString()).orElse("{}");

                log.info("工具调用: id={}, name={}, args={}", toolCallId, toolName, args);

                // 存储工具调用信息
                Map<String, Object> toolCallInfo = new HashMap<>();
                toolCallInfo.put("id", toolCallId);
                toolCallInfo.put("name", toolName);
                toolCallInfo.put("args", args);
                dynamicContext.getCurrentToolCalls().add(toolCallInfo);

                // 发送 SSE 工具调用事件
                sendToolCallEvent(emitter, toolCallId, toolName, "executing");

                roundToolCalls.incrementAndGet();
                dynamicContext.incrementTotalToolCalls();
            }
        }

        // 3. 处理工具响应（FunctionResponse）
        List<FunctionResponse> functionResponses = event.functionResponses();
        if (!functionResponses.isEmpty()) {
            for (FunctionResponse fr : functionResponses) {
                String responseId = fr.id().orElse("unknown");
                String responseName = fr.name().orElse("unknown");
                String responseContent = fr.response()
                        .map(r -> r.toString())
                        .orElse("(empty)");

                log.info("工具响应: id={}, name={}, content_length={}",
                        responseId, responseName, responseContent.length());

                // 截断过长的工具响应（避免 SSE 传输过大数据）
                String truncatedContent = truncateToolResponse(responseContent, 4000);

                // 存储工具响应信息
                Map<String, Object> toolResultInfo = new HashMap<>();
                toolResultInfo.put("id", responseId);
                toolResultInfo.put("name", responseName);
                toolResultInfo.put("content", truncatedContent);
                dynamicContext.getCurrentToolResults().add(toolResultInfo);

                // 发送 SSE 工具结果事件
                sendToolResultEvent(emitter, responseId, truncatedContent, "completed");
            }
        }

        // 4. 检查是否为最终响应（无更多工具调用）
        if (event.finalResponse()) {
            log.debug("收到最终响应事件");
        }

        // 5. 检查错误
        event.errorMessage().ifPresent(errMsg -> {
            log.error("ADK 事件错误: {}", errMsg);
            hasError.set(true);
            errorBuilder.append("ADK error: ").append(errMsg);
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  辅助方法
    // ═══════════════════════════════════════════════════════════════

    /**
     * 获取最新用户消息
     * <p>第一轮从 requestParameter，后续轮从 messageHistory
     */
    private String getLastUserMessage(ChatRequestDTO requestParameter,
                                      DefaultReActFactory.DynamicContext dynamicContext) {
        // 优先使用 requestParameter 的 message（首轮）
        if (requestParameter.getMessage() != null && !requestParameter.getMessage().isEmpty()) {
            return requestParameter.getMessage();
        }

        // 从 messageHistory 查找最后一条 user 消息
        List<Map<String, Object>> history = dynamicContext.getMessageHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            Map<String, Object> msg = history.get(i);
            if ("user".equals(msg.get("role"))) {
                return (String) msg.get("content");
            }
        }

        return "";
    }

    /**
     * 从 ADK Event 中提取文本内容
     */
    private List<String> extractTextParts(Event event) {
        List<String> texts = new java.util.ArrayList<>();
        event.content().ifPresent(content -> {
            content.parts().ifPresent(parts -> {
                for (Part part : parts) {
                    part.text().ifPresent(text -> {
                        if (!text.isBlank()) {
                            texts.add(text);
                        }
                    });
                }
            });
        });
        return texts;
    }

    /**
     * 截断过长的工具响应
     */
    private String truncateToolResponse(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "\n... (truncated, total " + content.length() + " chars)";
    }

    /**
     * 刷新文本缓冲到 SSE
     */
    private void flushTextBuffer(ResponseBodyEmitter emitter, StringBuilder textAccumulator) {
        if (textAccumulator.length() > 0) {
            sendTextEvent(emitter, textAccumulator.toString(), textAccumulator.toString());
        }
    }

}
