package cn.stackssh.domain.agent.service.enhance;

import cn.stackssh.domain.agent.model.valobj.enhance.ExtractedSignals;
import cn.stackssh.domain.agent.model.valobj.enhance.SearchContext;
import cn.stackssh.domain.agent.service.IIntentEnhancerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Slf4j
@Service
public class IntentEnhancerService implements IIntentEnhancerService {

    @Resource
    private SignalExtractor signalExtractor;

    @Resource
    private ContextSearch contextSearch;

    @Override
    public SearchContext enhance(String terminalSessionId, String userMessage) {
        // Step 1: 信号提取
        ExtractedSignals signals = signalExtractor.extract(userMessage);

        boolean hasSignals = !signals.getServiceNames().isEmpty()
                || !signals.getFilePaths().isEmpty()
                || !signals.getErrorPatterns().isEmpty()
                || !signals.getServerHosts().isEmpty();

        if (!hasSignals) {
            log.debug("未提取到有效信号，跳过上下文搜索");
            return SearchContext.builder().build();
        }

        log.info("提取到信号: 服务={}, 文件={}, 错误模式={}",
                signals.getServiceNames(), signals.getFilePaths(), signals.getErrorPatterns());

        // Step 2: 根据信号查找服务器上下文
        SearchContext result = contextSearch.searchBySignals(terminalSessionId, signals);

        log.info("上下文搜索完成: 服务状态={}, 文件数={}, 日志数={}",
                result.getServiceStatus().size(),
                result.getFileContents().size(),
                result.getRecentLogs().size());

        return result;
    }
}
