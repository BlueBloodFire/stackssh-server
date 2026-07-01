package cn.stackssh.domain.agent.service;

public interface IToolSummaryService {

    String buildSummary(String sessionId);

    void appendResult(String sessionId, String toolName, String resultContent);

    void rebuildFromHistory(String sessionId);
}
