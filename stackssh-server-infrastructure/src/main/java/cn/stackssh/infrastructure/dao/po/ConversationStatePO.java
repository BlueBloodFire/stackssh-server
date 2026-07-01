package cn.stackssh.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationStatePO {
    private Long id;
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
    private java.util.Date createdAt;
    private java.util.Date updatedAt;
}
