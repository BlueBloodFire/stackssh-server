package cn.stackssh.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRoundTraceEntity {

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
    private Long createdAt;
}
