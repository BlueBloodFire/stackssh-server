package cn.bugstack.ai.domain.agent.service.enhance;

import cn.bugstack.ai.domain.agent.model.valobj.enhance.ExtractedSignals;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class SignalExtractor {

    private static final List<String> KNOWN_SERVICES = List.of(
        "nginx", "apache", "httpd", "redis", "mysql", "mariadb", "postgres", "postgresql",
        "mongodb", "kafka", "rabbitmq", "docker", "containerd", "kubernetes", "kubelet",
        "jenkins", "gitlab", "elasticsearch", "kibana", "logstash", "prometheus", "grafana",
        "zookeeper", "etcd", "consul", "nacos", "tomcat", "spring", "node", "php-fpm",
        "sshd", "firewalld", "iptables", "crond", "rsyslogd"
    );

    private static final Pattern FILE_PATH_PATTERN = Pattern.compile(
        "(?:/[\\w.-]+)+\\.(?:conf|cfg|yml|yaml|properties|xml|json|log|sh|service|ini|cnf)");

    private static final Pattern IP_PATTERN = Pattern.compile(
        "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    private static final List<Pattern> ERROR_PATTERNS = List.of(
        Pattern.compile("(?i)(?:HTTP\\s*)?5\\d{2}"),
        Pattern.compile("(?i)(?:error|exception|fatal|panic|oom|segfault)[\\s:]?.{0,50}"),
        Pattern.compile("(?i)connection\\s+(?:refused|timed\\s*out|reset)"),
        Pattern.compile("(?i)permission\\s+denied"),
        Pattern.compile("(?i)no\\s+such\\s+file"),
        Pattern.compile("(?i)disk\\s+(?:full|space)"),
        Pattern.compile("(?i)port\\s+\\d+")
    );

    private static final List<String> LOG_LEVELS = List.of(
        "error", "warn", "warning", "fatal", "critical",
        "exception", "timeout", "refused", "denied", "failed", "oom"
    );

    private static final List<String> COMMAND_HINTS = List.of(
        "systemctl", "service", "journalctl", "tail", "grep",
        "awk", "sed", "find", "curl", "wget", "ping", "telnet", "netstat", "ss",
        "top", "htop", "free", "df", "du", "ps", "kill", "lsof", "iptables",
        "docker", "kubectl", "apt", "yum", "rpm"
    );

    public ExtractedSignals extract(String message) {
        String lower = message.toLowerCase();
        return ExtractedSignals.builder()
                .serverHosts(extractByPattern(IP_PATTERN, message))
                .filePaths(extractByPattern(FILE_PATH_PATTERN, message))
                .serviceNames(KNOWN_SERVICES.stream().filter(lower::contains).collect(Collectors.toList()))
                .commandHints(COMMAND_HINTS.stream().filter(lower::contains).collect(Collectors.toList()))
                .errorPatterns(extractErrorPatterns(message))
                .logKeywords(LOG_LEVELS.stream().filter(lower::contains).collect(Collectors.toList()))
                .build();
    }

    private List<String> extractByPattern(Pattern pattern, String text) {
        List<String> results = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) results.add(matcher.group());
        return results;
    }

    private List<String> extractErrorPatterns(String message) {
        List<String> found = new ArrayList<>();
        for (Pattern p : ERROR_PATTERNS) {
            Matcher m = p.matcher(message);
            while (m.find()) found.add(m.group());
        }
        return found;
    }
}
