package cn.stackssh.config;

import cn.stackssh.infrastructure.dao.ISshConnectionDAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 应用启动后处理：将数据库中遗留的 CONNECTED 状态重置为 DISCONNECTED
 * 原因：SSH Session 保存在 JVM 内存中，进程重启后全部销毁，DB 状态需同步
 */
@Slf4j
@Component
public class AppStartupListener {

    @Resource
    private ISshConnectionDAO sshConnectionDAO;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            sshConnectionDAO.resetAllConnectedToDisconnected();
            log.info("启动清理：已将数据库中所有 CONNECTED 的 SSH 连接状态重置为 DISCONNECTED");
        } catch (Exception e) {
            log.warn("启动清理失败（不影响启动）: {}", e.getMessage());
        }
    }
}
