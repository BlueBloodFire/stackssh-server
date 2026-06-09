package cn.stackssh.domain.agent.service;

import cn.stackssh.domain.agent.model.valobj.AiAgentConfigTableVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 运行时模型配置服务 - 存储每个 Agent 的当前配置，支持热更新
 */
@Slf4j
@Service
public class RuntimeModelConfigService {

    private final ConcurrentHashMap<String, AiAgentConfigTableVO> configMap = new ConcurrentHashMap<>();

    public void registerConfig(String agentId, AiAgentConfigTableVO config) {
        configMap.put(agentId, config);
        log.info("注册 Agent 运行时配置 agentId={}", agentId);
    }

    public AiAgentConfigTableVO getConfig(String agentId) {
        return configMap.get(agentId);
    }

    public Map<String, AiAgentConfigTableVO> getAllConfigs() {
        return Collections.unmodifiableMap(configMap);
    }

    public boolean containsConfig(String agentId) {
        return configMap.containsKey(agentId);
    }
}
