package cn.stackssh.infrastructure.adapter.repository;

import cn.stackssh.domain.agent.adapter.repository.IChatHistoryRepository;
import cn.stackssh.domain.agent.model.entity.ChatMessageEntity;
import cn.stackssh.domain.agent.model.entity.ChatSessionEntity;
import cn.stackssh.domain.agent.model.valobj.prompt.MilestoneVO;
import cn.stackssh.infrastructure.dao.IChatMessageDao;
import cn.stackssh.infrastructure.dao.IChatMilestoneDao;
import cn.stackssh.infrastructure.dao.IChatSessionDao;
import cn.stackssh.infrastructure.dao.po.ChatMessagePO;
import cn.stackssh.infrastructure.dao.po.ChatMilestonePO;
import cn.stackssh.infrastructure.dao.po.ChatSessionPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ChatHistoryRepository implements IChatHistoryRepository {

    @Resource
    private IChatSessionDao chatSessionDao;

    @Resource
    private IChatMessageDao chatMessageDao;

    @Resource
    private IChatMilestoneDao chatMilestoneDao;

    @Override
    public void saveSession(ChatSessionEntity session) {
        ChatSessionPO po = ChatSessionPO.builder()
                .id(session.getId())
                .agentId(session.getAgentId())
                .userId(session.getUserId())
                .title(session.getTitle())
                .messageCount(session.getMessageCount())
                .build();
        chatSessionDao.insert(po);
    }

    @Override
    public void saveMessage(ChatMessageEntity message) {
        ChatMessagePO po = ChatMessagePO.builder()
                .sessionId(message.getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .toolName(message.getToolName())
                .toolCallId(message.getToolCallId())
                .priority(message.getPriority())
                .tokenCount(message.getTokenCount())
                .build();
        chatMessageDao.insert(po);
        chatSessionDao.updateMessageCount(message.getSessionId());
    }

    @Override
    public List<ChatMessageEntity> getRecentMessages(String sessionId, int limit) {
        List<ChatMessagePO> pos = chatMessageDao.queryRecentBySessionId(sessionId, limit);
        if (pos == null || pos.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatMessagePO> reversedPos = new ArrayList<>(pos);
        Collections.reverse(reversedPos);

        return reversedPos.stream().map(po -> ChatMessageEntity.builder()
                .id(po.getId())
                .sessionId(po.getSessionId())
                .role(po.getRole())
                .content(po.getContent())
                .toolName(po.getToolName())
                .toolCallId(po.getToolCallId())
                .priority(po.getPriority())
                .tokenCount(po.getTokenCount())
                .createdAt(po.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageEntity> getMessagesWithBudget(String sessionId, int tokenBudget) {
        if (tokenBudget <= 0) {
            return Collections.emptyList();
        }

        int limit = 100;
        List<ChatMessageEntity> recent = getRecentMessages(sessionId, limit);
        while (shouldExpandHistory(recent, tokenBudget, limit)) {
            limit = Math.min(limit * 2, 2000);
            List<ChatMessageEntity> expanded = getRecentMessages(sessionId, limit);
            if (expanded.size() == recent.size()) {
                recent = expanded;
                break;
            }
            recent = expanded;
        }

        if (recent.isEmpty()) {
            return recent;
        }

        List<ChatMessageEntity> result = new ArrayList<>();
        int currentTokens = 0;
        for (int i = recent.size() - 1; i >= 0; i--) {
            ChatMessageEntity msg = recent.get(i);
            int tokens = msg.getTokenCount() != null && msg.getTokenCount() > 0
                    ? msg.getTokenCount()
                    : estimateTokens(msg.getContent());
            if (currentTokens + tokens > tokenBudget && !result.isEmpty()) {
                break;
            }
            result.add(0, msg);
            currentTokens += tokens;
        }

        return result;
    }

    @Override
    public void saveMilestone(String sessionId, MilestoneVO milestoneVO) {
        ChatMilestonePO po = ChatMilestonePO.builder()
                .sessionId(sessionId)
                .type(milestoneVO.getType().name())
                .content(milestoneVO.getContent())
                .build();
        chatMilestoneDao.insert(po);
    }

    @Override
    public List<MilestoneVO> getRecentMilestones(String sessionId, int limit) {
        List<ChatMilestonePO> pos = chatMilestoneDao.queryRecentBySessionId(sessionId, limit);
        if (pos == null || pos.isEmpty()) {
            return Collections.emptyList();
        }
        return pos.stream().map(po -> MilestoneVO.builder()
                .type(MilestoneVO.Type.valueOf(po.getType()))
                .content(po.getContent())
                .timestamp(po.getCreatedAt() != null ? po.getCreatedAt().getTime() : System.currentTimeMillis())
                .build()).collect(Collectors.toList());
    }

    private boolean shouldExpandHistory(List<ChatMessageEntity> recent, int tokenBudget, int currentLimit) {
        if (recent.isEmpty() || recent.size() < currentLimit || currentLimit >= 2000) {
            return false;
        }

        int totalTokens = 0;
        for (ChatMessageEntity message : recent) {
            totalTokens += message.getTokenCount() != null && message.getTokenCount() > 0
                    ? message.getTokenCount()
                    : estimateTokens(message.getContent());
        }
        return totalTokens < tokenBudget;
    }

    private int estimateTokens(String content) {
        return content == null ? 0 : Math.max(1, content.length() / 2);
    }
}
