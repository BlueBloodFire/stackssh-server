package cn.stackssh.domain.agent.service;

import cn.stackssh.domain.agent.model.valobj.enhance.SearchContext;

public interface IIntentEnhancerService {
    SearchContext enhance(String terminalSessionId, String userMessage);
}
