package cn.stackssh.api.dto;

import lombok.Data;

@Data
public class ConversationStateDiagnosticResponseDTO {

    private String sessionId;
    private String userId;
    private String agentId;

    private String currentIntent;
    private String taskSummary;
    private String intentSummary;
    private String toolSummary;

    private Integer turnCount;
    private Integer contextVersion;
    private Long lastRoundAt;
    private String lastTerminalSessionId;

    private boolean lastUserMessageDigestPresent;
    private boolean cachedSearchContextPresent;
    private boolean cachedRagChunksPresent;
    private Boolean lastEnhancementCacheHit;
    private String lastEnhancementCacheReason;

    private int cachedServiceStatusCount;
    private int cachedFileContentCount;
    private int cachedRecentLogCount;
    private int cachedRagChunkCount;
}
