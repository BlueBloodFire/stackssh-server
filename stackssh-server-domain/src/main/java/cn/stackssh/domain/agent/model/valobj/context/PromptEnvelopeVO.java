package cn.stackssh.domain.agent.model.valobj.context;

import cn.stackssh.domain.agent.model.entity.ConversationStateEntity;
import cn.stackssh.domain.agent.model.valobj.enhance.SearchContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 一次模型调用前的完整上下文载荷。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptEnvelopeVO {

    private String sessionId;
    private String userId;
    private String terminalSessionId;

    private String rawUserMessage;
    private String finalUserMessage;

    private ConversationStateEntity conversationState;
    private SearchContext searchContext;
    private List<String> ragChunks;
    private List<Map<String, Object>> trimmedHistory;
    private ContextBudgetVO budget;
    private Boolean enhancementCacheHit;
    private String enhancementCacheReason;

    private Integer estimatedTokens;
}
