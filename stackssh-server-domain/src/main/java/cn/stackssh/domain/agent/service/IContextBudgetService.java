package cn.stackssh.domain.agent.service;

import cn.stackssh.domain.agent.model.entity.ConversationStateEntity;
import cn.stackssh.domain.agent.model.valobj.context.ContextBudgetVO;
import cn.stackssh.domain.agent.model.valobj.enhance.SearchContext;

import java.util.List;

public interface IContextBudgetService {

    ContextBudgetVO allocate(
            String userMessage,
            ConversationStateEntity conversationState,
            SearchContext searchContext,
            List<String> ragChunks,
            int totalBudget
    );
}
