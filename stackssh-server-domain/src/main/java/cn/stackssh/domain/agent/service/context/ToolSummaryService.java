package cn.stackssh.domain.agent.service.context;

import cn.stackssh.domain.agent.adapter.repository.IChatHistoryRepository;
import cn.stackssh.domain.agent.model.entity.ChatMessageEntity;
import cn.stackssh.domain.agent.service.IToolSummaryService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.StringJoiner;

@Service
public class ToolSummaryService implements IToolSummaryService {

    @Resource
    private IChatHistoryRepository chatHistoryRepository;

    @Override
    public String buildSummary(String sessionId) {
        List<ChatMessageEntity> recent = chatHistoryRepository.getRecentMessages(sessionId, 20);
        StringJoiner joiner = new StringJoiner("\n");
        int count = 0;
        for (int i = recent.size() - 1; i >= 0; i--) {
            ChatMessageEntity msg = recent.get(i);
            if (!"tool".equals(msg.getRole())) {
                continue;
            }
            String name = msg.getToolName() != null ? msg.getToolName() : "tool";
            String content = msg.getContent() != null ? msg.getContent() : "";
            joiner.add(name + ": " + truncate(content, 120));
            count++;
            if (count >= 5) {
                break;
            }
        }
        return joiner.toString();
    }

    @Override
    public void appendResult(String sessionId, String toolName, String resultContent) {
        // 摘要按需从历史重建，避免维护第二份临时状态。
    }

    @Override
    public void rebuildFromHistory(String sessionId) {
        // 当前实现按 buildSummary 即时重建。
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
