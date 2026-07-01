package cn.stackssh.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单次工具执行记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionRecordEntity {

    private String toolCallId;
    private String toolName;
    private String resultContent;
    private String status;
    private Integer estimatedTokens;
}
