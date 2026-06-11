package cn.stackssh.domain.agent.service;

import cn.stackssh.domain.agent.model.valobj.prompt.PromptContextVO;

import java.util.List;
import java.util.Map;

/**
 * 上下文管理领域服务接口
 *
 * @author wangjin
 */
public interface IChatContextService {
    PromptContextVO buildPromptContext(String sessionId, String userId, String terminalSessionId, List<Map<String, Object>> messageHistory);
    List<Map<String, Object>> trimHistory(List<Map<String, Object>> history, int tokenBudget);
    void pushToolResult(String sessionId, String toolName, String result);
}
