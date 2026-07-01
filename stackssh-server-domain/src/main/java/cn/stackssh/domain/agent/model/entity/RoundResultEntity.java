package cn.stackssh.domain.agent.model.entity;

import cn.stackssh.domain.agent.model.valobj.enhance.SearchContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 一轮对话结束后的提交对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundResultEntity {

    private String sessionId;
    private String userId;
    private String agentId;
    private String terminalSessionId;

    private String rawUserMessage;
    private String assistantMessage;
    private List<String> ragChunks;

    private String currentIntent;
    private SearchContext searchContext;
    private Boolean enhancementCacheHit;
    private String enhancementCacheReason;

    private List<ToolExecutionRecordEntity> toolExecutions;

    private boolean success;
    private String errorMessage;
    private Integer totalSteps;
}
