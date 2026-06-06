package cn.bugstack.ai.domain.agent.model.valobj.enhance;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExtractedSignals {
    private List<String> serverHosts;
    private List<String> filePaths;
    private List<String> serviceNames;
    private List<String> commandHints;
    private List<String> errorPatterns;
    private List<String> logKeywords;
}
