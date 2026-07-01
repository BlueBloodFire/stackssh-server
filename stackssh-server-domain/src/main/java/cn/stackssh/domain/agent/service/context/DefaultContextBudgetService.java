package cn.stackssh.domain.agent.service.context;

import cn.stackssh.domain.agent.model.entity.ConversationStateEntity;
import cn.stackssh.domain.agent.model.valobj.context.ContextBudgetVO;
import cn.stackssh.domain.agent.model.valobj.enhance.SearchContext;
import cn.stackssh.domain.agent.service.IContextBudgetService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultContextBudgetService implements IContextBudgetService {

    @Override
    public ContextBudgetVO allocate(
            String userMessage,
            ConversationStateEntity conversationState,
            SearchContext searchContext,
            List<String> ragChunks,
            int totalBudget
    ) {
        int safeTotal = totalBudget > 0 ? totalBudget : 12000;

        int historyBudget = 5000;
        int taskBudget = 600;
        int intentBudget = 800;
        int toolBudget = 1800;
        int searchBudget = 2200;
        int ragBudget = 1600;

        if (searchContext != null && !searchContext.isEmpty()) {
            searchBudget += 400;
            historyBudget -= 200;
            ragBudget -= 200;
        }

        if (ragChunks != null && !ragChunks.isEmpty()) {
            ragBudget += 400;
            historyBudget -= 400;
        }

        if (conversationState != null && conversationState.getTurnCount() != null
                && conversationState.getTurnCount() > 20) {
            taskBudget += 200;
            intentBudget += 200;
            historyBudget -= 400;
        }

        return ContextBudgetVO.builder()
                .totalBudget(safeTotal)
                .historyBudget(Math.max(historyBudget, 2000))
                .taskBudget(Math.max(taskBudget, 300))
                .intentBudget(Math.max(intentBudget, 400))
                .toolBudget(Math.max(toolBudget, 800))
                .searchBudget(Math.max(searchBudget, 600))
                .ragBudget(Math.max(ragBudget, 600))
                .build();
    }
}
