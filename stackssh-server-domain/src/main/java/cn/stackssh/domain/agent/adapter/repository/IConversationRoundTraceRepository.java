package cn.stackssh.domain.agent.adapter.repository;

import cn.stackssh.domain.agent.model.entity.ConversationRoundTraceEntity;

import java.util.List;

public interface IConversationRoundTraceRepository {

    void save(ConversationRoundTraceEntity entity);

    List<ConversationRoundTraceEntity> queryRecentBySessionId(String sessionId, int limit);
}
