package cn.bugstack.ai.config;

import cn.bugstack.ai.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.bugstack.ai.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import cn.bugstack.ai.domain.agent.service.IArmoryService;
import cn.bugstack.ai.domain.agent.service.RuntimeModelConfigService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@EnableConfigurationProperties(AiAgentAutoConfigProperties.class)
public class AiAgentAutoConfig implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;

    @Resource
    private IArmoryService armoryService;

    @Resource
    private RuntimeModelConfigService runtimeModelConfigService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            List<AiAgentConfigTableVO> tables = new ArrayList<>(aiAgentAutoConfigProperties.getTables().values());
            log.info("Ai Agent 智能体装配 {}", JSON.toJSONString(tables));

            armoryService.acceptArmoryAgents(tables);

            // 装配完成后，将配置存入运行时服务，供后续热更新使用
            for (AiAgentConfigTableVO table : tables) {
                if (table.getAgent() != null && table.getAgent().getAgentId() != null) {
                    runtimeModelConfigService.registerConfig(table.getAgent().getAgentId(), table);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
