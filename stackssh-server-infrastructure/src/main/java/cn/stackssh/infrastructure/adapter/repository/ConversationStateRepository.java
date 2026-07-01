package cn.stackssh.infrastructure.adapter.repository;

import cn.stackssh.domain.agent.adapter.repository.IConversationStateRepository;
import cn.stackssh.domain.agent.model.entity.ConversationStateEntity;
import cn.stackssh.infrastructure.dao.IConversationStateDao;
import cn.stackssh.infrastructure.dao.po.ConversationStatePO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationStateRepository implements IConversationStateRepository {

    @Resource
    private IConversationStateDao conversationStateDao;

    @Override
    public ConversationStateEntity findBySessionId(String sessionId) {
        ConversationStatePO po = conversationStateDao.queryBySessionId(sessionId);
        return po == null ? null : toEntity(po);
    }

    @Override
    public void save(ConversationStateEntity entity) {
        conversationStateDao.insert(toPo(entity));
    }

    @Override
    public void upsert(ConversationStateEntity entity) {
        conversationStateDao.upsert(toPo(entity));
    }

    @Override
    public void updateTaskSummary(String sessionId, String taskSummary) {
        conversationStateDao.updateTaskSummary(sessionId, taskSummary);
    }

    @Override
    public void updateIntentState(String sessionId, String currentIntent, String intentSummary, Integer turnCount, Long lastRoundAt) {
        conversationStateDao.updateIntentState(sessionId, currentIntent, intentSummary, turnCount, lastRoundAt);
    }

    @Override
    public void updateToolSummary(String sessionId, String toolSummary, Long lastRoundAt) {
        conversationStateDao.updateToolSummary(sessionId, toolSummary, lastRoundAt);
    }

    @Override
    public void updateTerminalBinding(String sessionId, String terminalSessionId) {
        conversationStateDao.updateTerminalBinding(sessionId, terminalSessionId);
    }

    private ConversationStateEntity toEntity(ConversationStatePO po) {
        return ConversationStateEntity.builder()
                .sessionId(po.getSessionId())
                .userId(po.getUserId())
                .agentId(po.getAgentId())
                .taskSummary(po.getTaskSummary())
                .currentIntent(po.getCurrentIntent())
                .intentSummary(po.getIntentSummary())
                .toolSummary(po.getToolSummary())
                .lastUserMessageDigest(po.getLastUserMessageDigest())
                .cachedSearchContext(po.getCachedSearchContext())
                .cachedRagChunks(po.getCachedRagChunks())
                .lastEnhancementCacheHit(po.getLastEnhancementCacheHit())
                .lastEnhancementCacheReason(po.getLastEnhancementCacheReason())
                .lastTerminalSessionId(po.getLastTerminalSessionId())
                .turnCount(po.getTurnCount())
                .contextVersion(po.getContextVersion())
                .lastRoundAt(po.getLastRoundAt())
                .build();
    }

    private ConversationStatePO toPo(ConversationStateEntity entity) {
        return ConversationStatePO.builder()
                .sessionId(entity.getSessionId())
                .userId(entity.getUserId())
                .agentId(entity.getAgentId())
                .taskSummary(entity.getTaskSummary())
                .currentIntent(entity.getCurrentIntent())
                .intentSummary(entity.getIntentSummary())
                .toolSummary(entity.getToolSummary())
                .lastUserMessageDigest(entity.getLastUserMessageDigest())
                .cachedSearchContext(entity.getCachedSearchContext())
                .cachedRagChunks(entity.getCachedRagChunks())
                .lastEnhancementCacheHit(entity.getLastEnhancementCacheHit())
                .lastEnhancementCacheReason(entity.getLastEnhancementCacheReason())
                .lastTerminalSessionId(entity.getLastTerminalSessionId())
                .turnCount(entity.getTurnCount())
                .contextVersion(entity.getContextVersion())
                .lastRoundAt(entity.getLastRoundAt())
                .build();
    }
}
