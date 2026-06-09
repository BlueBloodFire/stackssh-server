package cn.stackssh.domain.agent.model.valobj.prompt;

import cn.stackssh.domain.agent.model.valobj.enhance.SearchContext;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PromptContextVO {

    private String serverInfo;
    private String osInfo;
    private String currentUser;
    private String currentDirectory;

    private List<String> recentCommands;

    private List<MilestoneVO> milestoneVOS;

    private String toolResultSummary;

    private String taskDescription;

    /** Phase 4: 意图增强搜索上下文（服务状态、配置文件、日志） */
    private SearchContext searchContext;
}
