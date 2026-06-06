package cn.bugstack.ai.domain.ssh.model.valobj;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 危险命令黑名单配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "terminal.safety")
public class DangerousCommandProperties {
    private List<String> dangerousCommands = new ArrayList<>();
}
