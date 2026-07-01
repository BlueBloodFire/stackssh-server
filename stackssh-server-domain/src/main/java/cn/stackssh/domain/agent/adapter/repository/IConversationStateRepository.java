package cn.stackssh.domain.agent.adapter.repository;

import cn.stackssh.domain.agent.model.entity.ConversationStateEntity;

public interface IConversationStateRepository {

    ConversationStateEntity findBySessionId(String sessionId);

    void save(ConversationStateEntity entity);

    void upsert(ConversationStateEntity entity);

    void updateTaskSummary(String sessionId, String taskSummary);

    void updateIntentState(
            String sessionId,
            String currentIntent,
            String intentSummary,
            Integer turnCount,
            Long lastRoundAt
    );

    void updateToolSummary(
            String sessionId,
            String toolSummary,
            Long lastRoundAt
    );

    void updateTerminalBinding(String sessionId, String terminalSessionId);
}
