package cn.stackssh.domain.agent.service.prompt;

import cn.stackssh.domain.agent.model.valobj.context.PromptEnvelopeVO;
import cn.stackssh.domain.agent.model.valobj.prompt.PromptContextVO;
import cn.stackssh.domain.agent.service.IPromptService;
import cn.stackssh.domain.agent.service.prompt.dynamic.DynamicPromptBuilder;
import cn.stackssh.domain.agent.service.prompt.dynamic.MilestoneTracker;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PromptService implements IPromptService {

    @Resource
    private DynamicPromptBuilder dynamicPromptBuilder;

    @Resource
    private MilestoneTracker milestoneTracker;

    @Override
    public void detectAndRecordMilestone(String sessionId, String role, String content) {
        milestoneTracker.detectAndRecord(sessionId, role, content);
    }

    @Override
    public String buildFinalUserMessage(PromptEnvelopeVO envelope, PromptContextVO promptContextVO) {
        String prefix = dynamicPromptBuilder.buildMessagePrefix(promptContextVO);
        StringBuilder sb = new StringBuilder();

        if (prefix != null && !prefix.isBlank()) {
            sb.append(prefix).append("\n---\n");
        }

        if (envelope.getRagChunks() != null && !envelope.getRagChunks().isEmpty()) {
            sb.append("[知识库信息]\n")
                    .append(String.join("\n---\n", envelope.getRagChunks()))
                    .append("\n---\n");
        }

        sb.append(envelope.getRawUserMessage());
        return sb.toString();
    }

    @Override
    public void clearMilestones(String sessionId) {
        milestoneTracker.clear(sessionId);
    }
}
