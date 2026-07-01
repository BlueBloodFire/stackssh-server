package cn.stackssh.api.dto;

import lombok.Data;

@Data
public class ConversationRoundTraceItemDTO {

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
