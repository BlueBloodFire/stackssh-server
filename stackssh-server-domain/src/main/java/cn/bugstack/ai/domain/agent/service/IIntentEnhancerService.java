package cn.bugstack.ai.domain.agent.service;

import cn.bugstack.ai.domain.agent.model.valobj.enhance.SearchContext;

public interface IIntentEnhancerService {
    SearchContext enhance(String terminalSessionId, String userMessage);
}
