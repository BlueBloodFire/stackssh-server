package cn.stackssh.domain.agent.service;

import cn.stackssh.domain.agent.model.valobj.context.PromptEnvelopeVO;
import cn.stackssh.domain.agent.model.valobj.prompt.PromptContextVO;

public interface IPromptService {

    void detectAndRecordMilestone(String sessionId, String role, String content);

    String buildFinalUserMessage(PromptEnvelopeVO envelope, PromptContextVO promptContextVO);

    void clearMilestones(String sessionId);
}
