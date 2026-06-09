package cn.stackssh.domain.agent.service;

import cn.stackssh.domain.agent.model.valobj.intent.IntentResultVO;

public interface IIntentService {
    IntentResultVO classify(String sessionId, String userId, String message);
}
