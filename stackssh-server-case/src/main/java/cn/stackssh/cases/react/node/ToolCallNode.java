package cn.stackssh.cases.react.node;

import cn.stackssh.api.dto.ChatRequestDTO;
import cn.stackssh.api.dto.ReActResultDTO;
import cn.stackssh.cases.react.AbstractAIAgentReActSupport;
import cn.stackssh.cases.react.factory.DefaultReActFactory;
import cn.stackssh.domain.agent.service.IChatContextService;
import cn.stackssh.domain.agent.service.IConversationRoundService;
import cn.stackssh.domain.agent.service.armory.matter.tools.SshExecuteAdkTool;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("reactToolCallNode")
public class ToolCallNode extends AbstractAIAgentReActSupport {

    @Resource
    private SshExecuteAdkTool sshExecuteAdkTool;

    @Resource
    private IChatContextService chatContextService;

    @Resource
    private IConversationRoundService conversationRoundService;

    @Override
    protected ReActResultDTO doApply(ChatRequestDTO requestParameter, DefaultReActFactory.DynamicContext dynamicContext) throws Exception {
        List<Map<String, Object>> toolCalls = dynamicContext.getCurrentToolCalls();
        List<Map<String, Object>> toolResults = dynamicContext.getCurrentToolResults();

        if (toolCalls == null || toolCalls.isEmpty()) {
            log.info("ReAct ToolCallNode - no tool calls to process");
            return router(requestParameter, dynamicContext);
        }

        log.info("ReAct ToolCallNode - processing {} tool calls, {} results already present",
                toolCalls.size(), toolResults != null ? toolResults.size() : 0);

        ResponseBodyEmitter emitter = dynamicContext.getEmitter();
        boolean adkAutoExecuted = toolResults != null && !toolResults.isEmpty();

        if (adkAutoExecuted) {
            handleAdkToolResults(dynamicContext, toolCalls, toolResults);
        } else {
            handleManualToolExecution(dynamicContext, toolCalls, emitter);
        }

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ChatRequestDTO, DefaultReActFactory.DynamicContext, ReActResultDTO> get(
            ChatRequestDTO requestParameter,
            DefaultReActFactory.DynamicContext dynamicContext) throws Exception {

        List<Map<String, Object>> toolCalls = dynamicContext.getCurrentToolCalls();
        if (toolCalls != null && !toolCalls.isEmpty()) {
            if (dynamicContext.getStep() >= dynamicContext.getMaxSteps()) {
                log.info("ReAct ToolCallNode - max steps reached: {}", dynamicContext.getMaxSteps());
                dynamicContext.setStopReason("max_steps");
                return getBean("reactLoopDecisionNode");
            }
            return getBean("reactAiCallNode");
        }

        return getBean("reactLoopDecisionNode");
    }

    private void handleAdkToolResults(DefaultReActFactory.DynamicContext dynamicContext,
                                      List<Map<String, Object>> toolCalls,
                                      List<Map<String, Object>> toolResults) {
        Map<String, Map<String, Object>> resultMap = new HashMap<>();
        for (Map<String, Object> result : toolResults) {
            String id = (String) result.get("id");
            if (id != null) {
                resultMap.put(id, result);
            }
        }

        for (Map<String, Object> toolCall : toolCalls) {
            String toolCallId = (String) toolCall.get("id");
            String toolName = (String) toolCall.get("name");
            Map<String, Object> matchedResult = resultMap.get(toolCallId);
            if (matchedResult != null) {
                String content = (String) matchedResult.get("content");
                log.info("ADK tool result observed: id={}, name={}, resultLength={}",
                        toolCallId, toolName, content != null ? content.length() : 0);
            } else {
                log.warn("ADK tool result missing: id={}, name={}", toolCallId, toolName);
            }
        }

        dynamicContext.getCurrentToolCalls().clear();
    }

    private void handleManualToolExecution(DefaultReActFactory.DynamicContext dynamicContext,
                                           List<Map<String, Object>> toolCalls,
                                           ResponseBodyEmitter emitter) throws Exception {
        Map<String, Map<String, Object>> existingResults = indexResults(dynamicContext.getCurrentToolResults());
        for (Map<String, Object> toolCall : toolCalls) {
            String toolCallId = (String) toolCall.get("id");
            String toolName = (String) toolCall.get("name");
            String argsStr = (String) toolCall.get("args");

            if (toolCallId == null || toolName == null) {
                log.warn("Incomplete tool call payload: {}", toolCall);
                continue;
            }
            if (existingResults.containsKey(toolCallId)) {
                log.debug("Skip duplicated manual tool execution: {}", toolCallId);
                continue;
            }

            sendToolCallEvent(emitter, toolCallId, toolName, "executing");

            String resultContent;
            String status = "success";
            try {
                resultContent = executeTool(toolName, argsStr);
            } catch (Exception e) {
                log.error("Manual tool execution failed: {}", toolName, e);
                resultContent = "Error executing tool '" + toolName + "': " + e.getMessage();
                status = "error";
            }

            resultContent = truncateToolResponse(resultContent, 4000);

            Map<String, Object> toolResult = new HashMap<>();
            toolResult.put("id", toolCallId);
            toolResult.put("name", toolName);
            toolResult.put("content", resultContent);
            toolResult.put("status", status);
            dynamicContext.getCurrentToolResults().add(toolResult);
            existingResults.put(toolCallId, toolResult);

            dynamicContext.appendToolMessage(toolCallId, resultContent);
            chatContextService.pushToolResult(dynamicContext.getSessionId(), toolName, resultContent);
            conversationRoundService.commitToolResult(
                    dynamicContext.getSessionId(),
                    toolName,
                    toolCallId,
                    resultContent,
                    status,
                    resultContent.length() / 2
            );

            sendToolResultEvent(emitter, toolCallId, resultContent, status);
        }
    }

    private String executeTool(String toolName, String argsStr) throws Exception {
        switch (toolName) {
            case "executeCommand":
            case "execute_command":
            case "run_command":
                return executeSshTool(argsStr);
            default:
                return "Unknown tool: " + toolName + ". Available tools: executeCommand";
        }
    }

    private String executeSshTool(String argsStr) throws Exception {
        String command = parseToolArg(argsStr, "command");
        if (command == null || command.isBlank()) {
            return "Error: missing 'command' argument";
        }

        Map<String, Object> result = sshExecuteAdkTool.executeCommand(command, null);
        return formatSshResult(result);
    }

    private String formatSshResult(Map<String, Object> result) {
        if (result == null) {
            return "No result";
        }

        StringBuilder sb = new StringBuilder();
        Object output = result.get("output");
        if (output != null && !output.toString().isEmpty()) {
            sb.append(output);
        }

        Object error = result.get("error");
        if (error != null && !error.toString().isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append("[ERROR] ").append(error);
        }

        Object exitCode = result.get("exitCode");
        if (exitCode != null) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append("[Exit code: ").append(exitCode).append("]");
        }

        return !sb.isEmpty() ? sb.toString() : "Command executed with no output";
    }

    private String parseToolArg(String argsStr, String key) {
        if (argsStr == null || argsStr.isBlank()) {
            return null;
        }

        try {
            Map<String, Object> args = objectMapper.readValue(
                    argsStr,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
            );
            Object value = args.get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("Failed to parse tool args: {}", e.getMessage());
            String pattern = "\"" + key + "\"";
            int idx = argsStr.indexOf(pattern);
            if (idx >= 0) {
                int colonIdx = argsStr.indexOf(":", idx + pattern.length());
                if (colonIdx >= 0) {
                    String remaining = argsStr.substring(colonIdx + 1).trim();
                    if (remaining.startsWith("\"")) {
                        int endQuote = remaining.indexOf("\"", 1);
                        if (endQuote > 0) {
                            return remaining.substring(1, endQuote);
                        }
                    }
                }
            }
            return null;
        }
    }

    private String truncateToolResponse(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "\n... (truncated, total " + content.length() + " chars)";
    }

    private Map<String, Map<String, Object>> indexResults(List<Map<String, Object>> toolResults) {
        Map<String, Map<String, Object>> resultMap = new HashMap<>();
        if (toolResults == null) {
            return resultMap;
        }
        for (Map<String, Object> result : toolResults) {
            String id = (String) result.get("id");
            if (id != null && !id.isEmpty()) {
                resultMap.put(id, result);
            }
        }
        return resultMap;
    }

}
