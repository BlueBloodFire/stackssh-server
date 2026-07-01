package cn.stackssh.domain.agent.model.valobj.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上下文预算分配。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextBudgetVO {

    private int totalBudget;
    private int historyBudget;
    private int taskBudget;
    private int intentBudget;
    private int toolBudget;
    private int searchBudget;
    private int ragBudget;
}
