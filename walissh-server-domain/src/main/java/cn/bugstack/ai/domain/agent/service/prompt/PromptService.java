package cn.bugstack.ai.domain.agent.service.prompt;

import cn.bugstack.ai.domain.agent.model.valobj.prompt.MilestoneVO;
import cn.bugstack.ai.domain.agent.model.valobj.prompt.PromptContextVO;
import cn.bugstack.ai.domain.agent.service.IPromptService;
import cn.bugstack.ai.domain.agent.service.prompt.dynamic.DynamicPromptBuilder;
import cn.bugstack.ai.domain.agent.service.prompt.dynamic.MilestoneTracker;
import cn.bugstack.ai.domain.ssh.service.ISshTerminalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 提示词服务
 * <p>
 * 组合 DynamicPromptBuilder、MilestoneTracker、ISshTerminalService，
 * 向 case 层提供统一的提示词领域能力。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2026/5/5 22:18
 */
@Slf4j
@Service
public class PromptService implements IPromptService {

    @Resource
    private DynamicPromptBuilder dynamicPromptBuilder;

    @Resource
    private MilestoneTracker milestoneTracker;

    @Resource
    private ISshTerminalService sshTerminalService;

    @Override
    public void detectAndRecordMilestone(String sessionId, String role, String content) {
        milestoneTracker.detectAndRecord(sessionId, role, content);
    }

    @Override
    public String buildEnrichedMessage(String userMessage, String sessionId, String terminalSessionId, List<String> recentCommands) {
        // 1. 从 SSH 终端采集环境信息
        PromptContextVO promptContextVO = buildPromptContext(sessionId, terminalSessionId, recentCommands);

        // 2. 生成消息前缀
        String prefix = dynamicPromptBuilder.buildMessagePrefix(promptContextVO);

        if (prefix.isEmpty()) {
            return userMessage;
        }

        return prefix + "\n---\n" + userMessage;
    }

    @Override
    public void clearMilestones(String sessionId) {
        milestoneTracker.clear(sessionId);
    }

    private PromptContextVO buildPromptContext(String sessionId, String terminalSessionId, List<String> recentCommands) {
        String osInfo = "";
        String currentUser = "";
        String currentDirectory = "";

        if (terminalSessionId != null && !terminalSessionId.isEmpty()) {
            try {
                String raw = sshTerminalService.executeCommand(terminalSessionId, "uname -srm");
                osInfo = raw != null ? raw.trim() : "";
            } catch (Exception e) {
                log.debug("获取 OS 信息失败: {}", e.getMessage());
            }
            try {
                String raw = sshTerminalService.executeCommand(terminalSessionId, "whoami");
                currentUser = raw != null ? raw.trim() : "";
            } catch (Exception e) {
                log.debug("获取用户信息失败: {}", e.getMessage());
            }
            try {
                String raw = sshTerminalService.executeCommand(terminalSessionId, "pwd");
                currentDirectory = raw != null ? raw.trim() : "";
            } catch (Exception e) {
                log.debug("获取工作目录失败: {}", e.getMessage());
            }
        }

        List<MilestoneVO> milestoneVOS = milestoneTracker.getRecent(sessionId, 10);

        return PromptContextVO.builder()
                .osInfo(osInfo)
                .currentUser(currentUser)
                .currentDirectory(currentDirectory)
                .recentCommands(recentCommands)
                .milestoneVOS(milestoneVOS)
                .build();
    }
}
