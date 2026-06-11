package cn.stackssh.domain.agent.service.enhance;

import cn.stackssh.domain.agent.model.valobj.enhance.ExtractedSignals;
import cn.stackssh.domain.agent.model.valobj.enhance.SearchContext;
import cn.stackssh.domain.ssh.service.ISshTerminalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class ContextSearch {

    @Resource
    private ISshTerminalService sshTerminalService;

    public SearchContext searchBySignals(String terminalSessionId, ExtractedSignals signals) {
        if (terminalSessionId == null || terminalSessionId.isEmpty()) {
            return SearchContext.builder().build();
        }

        SearchContext.SearchContextBuilder builder = SearchContext.builder();

        // 1. 查询服务状态
        if (!signals.getServiceNames().isEmpty()) {
            Map<String, String> statusMap = new LinkedHashMap<>();
            for (String svc : signals.getServiceNames()) {
                String status = safeExec(terminalSessionId,
                    "systemctl is-active " + svc + " 2>/dev/null || " +
                    "service " + svc + " status 2>&1 | head -3");
                if (!status.isEmpty()) {
                    statusMap.put(svc, status);
                }
            }
            builder.serviceStatus(statusMap);
        }

        // 2. 读取相关配置文件（取前 50 行）
        if (!signals.getFilePaths().isEmpty()) {
            Map<String, String> contentMap = new LinkedHashMap<>();
            for (String path : signals.getFilePaths()) {
                String content = safeExec(terminalSessionId, "head -50 " + path + " 2>/dev/null");
                if (!content.isEmpty() && !content.contains("No such file")) {
                    contentMap.put(path, content);
                }
            }
            builder.fileContents(contentMap);
        }

        // 3. 搜索最近日志
        if (!signals.getErrorPatterns().isEmpty() || !signals.getLogKeywords().isEmpty()) {
            Map<String, String> logMap = new LinkedHashMap<>();
            for (String svc : signals.getServiceNames()) {
                String logs = safeExec(terminalSessionId,
                    "journalctl -u " + svc + " --no-pager -n 30 --since '1 hour ago' 2>/dev/null || " +
                    "tail -30 /var/log/" + svc + "/*.log 2>/dev/null || " +
                    "tail -30 /var/log/" + svc + ".log 2>/dev/null");
                if (!logs.isEmpty()) logMap.put(svc, logs);
            }
            // 无指定服务但有错误模式，查系统日志
            if (logMap.isEmpty() && !signals.getErrorPatterns().isEmpty()) {
                String sysLogs = safeExec(terminalSessionId,
                    "dmesg --time-format iso -T 2>/dev/null | tail -30 || dmesg | tail -30");
                if (!sysLogs.isEmpty()) logMap.put("system", sysLogs);
            }
            builder.recentLogs(logMap);
        }

        return builder.build();
    }

    private String safeExec(String sessionId, String cmd) {
        try {
            String result = sshTerminalService.executeCommand(sessionId, cmd);
            return result != null ? result.trim() : "";
        } catch (Exception e) {
            log.debug("ContextSearch safeExec failed: {}", e.getMessage());
            return "";
        }
    }
}
