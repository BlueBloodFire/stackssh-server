package cn.stackssh.infrastructure.adapter.repository;

import cn.stackssh.domain.agent.adapter.repository.IConversationRoundTraceRepository;
import cn.stackssh.domain.agent.model.entity.ConversationRoundTraceEntity;
import cn.stackssh.infrastructure.dao.IConversationRoundTraceDao;
import cn.stackssh.infrastructure.dao.po.ConversationRoundTracePO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class ConversationRoundTraceRepository implements IConversationRoundTraceRepository {

    @Resource
    private IConversationRoundTraceDao conversationRoundTraceDao;

    @Override
    public void save(ConversationRoundTraceEntity entity) {
        conversationRoundTraceDao.insert(toPo(entity));
    }

    @Override
    public List<ConversationRoundTraceEntity> queryRecentBySessionId(String sessionId, int limit) {
        List<ConversationRoundTracePO> pos = conversationRoundTraceDao.queryRecentBySessionId(sessionId, limit);
        if (pos == null || pos.isEmpty()) {
            return Collections.emptyList();
        }
        return pos.stream().map(this::toEntity).toList();
    }

    private ConversationRoundTracePO toPo(ConversationRoundTraceEntity entity) {
        return ConversationRoundTracePO.builder()
                .sessionId(entity.getSessionId())
                .userId(entity.getUserId())
                .agentId(entity.getAgentId())
                .turnNumber(entity.getTurnNumber())
                .currentIntent(entity.getCurrentIntent())
                .enhancementCacheHit(entity.getEnhancementCacheHit())
                .enhancementCacheReason(entity.getEnhancementCacheReason())
                .rawUserMessage(entity.getRawUserMessage())
                .assistantMessage(entity.getAssistantMessage())
                .toolSummary(entity.getToolSummary())
                .success(entity.getSuccess())
                .errorMessage(entity.getErrorMessage())
                .totalSteps(entity.getTotalSteps())
                .build();
    }

    private ConversationRoundTraceEntity toEntity(ConversationRoundTracePO po) {
        return ConversationRoundTraceEntity.builder()
                .sessionId(po.getSessionId())
                .userId(po.getUserId())
                .agentId(po.getAgentId())
                .turnNumber(po.getTurnNumber())
                .currentIntent(po.getCurrentIntent())
                .enhancementCacheHit(po.getEnhancementCacheHit())
                .enhancementCacheReason(po.getEnhancementCacheReason())
                .rawUserMessage(po.getRawUserMessage())
                .assistantMessage(po.getAssistantMessage())
                .toolSummary(po.getToolSummary())
                .success(po.getSuccess())
                .errorMessage(po.getErrorMessage())
                .totalSteps(po.getTotalSteps())
                .createdAt(po.getCreatedAt() != null ? po.getCreatedAt().getTime() : null)
                .build();
    }
}
