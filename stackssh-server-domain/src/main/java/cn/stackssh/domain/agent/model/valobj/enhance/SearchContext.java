package cn.stackssh.domain.agent.model.valobj.enhance;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchContext {
    @Builder.Default
    private Map<String, String> serviceStatus = Map.of();
    @Builder.Default
    private Map<String, String> fileContents = Map.of();
    @Builder.Default
    private Map<String, String> recentLogs = Map.of();

    public boolean isEmpty() {
        return serviceStatus.isEmpty() && fileContents.isEmpty() && recentLogs.isEmpty();
    }
}
