package cn.stackssh.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话级状态快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationStateEntity {

    private String sessionId;
    private String userId;
    private String agentId;

    private String taskSummary;
    private String currentIntent;
    private String intentSummary;
    private String toolSummary;
    private String lastUserMessageDigest;
    private String cachedSearchContext;
    private String cachedRagChunks;
    private Boolean lastEnhancementCacheHit;
    private String lastEnhancementCacheReason;

    private String lastTerminalSessionId;

    private Integer turnCount;
    private Integer contextVersion;

    private Long lastRoundAt;
}
