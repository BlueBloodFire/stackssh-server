package cn.stackssh.domain.agent.service;

import cn.stackssh.domain.agent.model.valobj.enhance.SearchContext;

import java.util.List;
import java.util.Map;

/**
 * 提示词领域服务接口
 * <p>
 * 封装动态 Prompt 构建、里程碑追踪、环境信息采集等能力，
 * 使 case 层仅依赖此接口，不直接接触 DynamicPromptBuilder / MilestoneTracker 等内部组件。
 *
 * @author wangjin
 * 2026/5/5 22:16
 */
public interface IPromptService {

    void detectAndRecordMilestone(String sessionId, String role, String content);

    /**
     * 构建注入了动态上下文的用户消息
     *
     * @param userMessage        原始用户消息
     * @param sessionId          对话会话 ID
     * @param terminalSessionId  SSH 终端会话 ID（可为 null）
     * @param recentCommands     最近执行的命令列表
     * @param messageHistory     对话历史记录
     * @param searchContext      Phase 4 意图增强结果（可为 null）
     */
    String buildEnrichedMessage(String userMessage, String sessionId, String terminalSessionId,
                                List<String> recentCommands, List<Map<String, Object>> messageHistory,
                                SearchContext searchContext);

    void clearMilestones(String sessionId);
}
