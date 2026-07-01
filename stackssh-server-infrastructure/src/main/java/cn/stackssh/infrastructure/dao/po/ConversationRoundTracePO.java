package cn.stackssh.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRoundTracePO {

    private Long id;
    private String sessionId;
    private String userId;
    private String agentId;
    private Integer turnNumber;
    private String currentIntent;
    private Boolean enhancementCacheHit;
    private String enhancementCacheReason;
    private String rawUserMessage;
    private String assistantMessage;
    private String toolSummary;
    private Boolean success;
    private String errorMessage;
    private Integer totalSteps;
    private java.util.Date createdAt;
}
