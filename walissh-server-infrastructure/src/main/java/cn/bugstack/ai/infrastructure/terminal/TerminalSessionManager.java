package cn.bugstack.ai.infrastructure.terminal;

import cn.bugstack.ai.domain.ssh.adapter.session.ISshSessionService;
import cn.bugstack.ai.domain.ssh.adapter.terminal.ITerminalSessionService;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 终端会话管理器
 * 基础设施层实现，管理 Shell 通道的创建、读写、关闭
 *
 * @author waissh dev
 */
@Slf4j
@Component
public class TerminalSessionManager implements ITerminalSessionService {

    @Resource
    private ISshSessionService sshSessionService;

    /** sessionId -> Shell 通道 */
    private final Map<String, ChannelShell> channels = new ConcurrentHashMap<>();

    /** sessionId -> 输出流 */
    private final Map<String, OutputStream> outputStreams = new ConcurrentHashMap<>();

    /** sessionId -> 输入流 */
    private final Map<String, InputStream> inputStreams = new ConcurrentHashMap<>();

    /** sessionId -> 未读输出缓冲区 */
    private final Map<String, StringBuilder> outputBuffers = new ConcurrentHashMap<>();

    /** sessionId -> 读取线程是否存活 */
    private final Map<String, Boolean> readerAlive = new ConcurrentHashMap<>();

    @Override
    public String openTerminal(String connectionId, int cols, int rows) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        try {
            Session session = sshSessionService.getSession(connectionId);
            if (session == null || !session.isConnected()) {
                throw new IllegalStateException("SSH会话不可用 connectionId=" + connectionId);
            }

            ChannelShell channel = (ChannelShell) session.openChannel("shell");
            channel.setPty(true);
            channel.setPtySize(cols, rows, 480, 640);

            InputStream in = channel.getInputStream();
            OutputStream out = channel.getOutputStream();

            channel.connect(5000);

            channels.put(sessionId, channel);
            inputStreams.put(sessionId, in);
            outputStreams.put(sessionId, out);
            outputBuffers.put(sessionId, new StringBuilder());

            // 启动输出读取线程，持续读取 shell 输出到缓冲区
            startOutputReader(sessionId, in);

            log.info("终端会话打开成功 sessionId={} connectionId={}", sessionId, connectionId);
            return sessionId;

        } catch (Exception e) {
            log.error("打开终端会话失败 connectionId={}", connectionId, e);
            cleanup(sessionId);
            throw new RuntimeException("打开终端失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void write(String sessionId, String command) {
        OutputStream out = outputStreams.get(sessionId);
        if (out == null) {
            throw new IllegalArgumentException("终端会话不存在或已关闭 sessionId=" + sessionId);
        }

        try {
            out.write(command.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            log.error("写入终端失败 sessionId={}", sessionId, e);
            throw new RuntimeException("写入终端失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String read(String sessionId) {
        StringBuilder buffer = outputBuffers.get(sessionId);
        if (buffer == null) {
            throw new IllegalArgumentException("终端会话不存在或已关闭 sessionId=" + sessionId);
        }

        Boolean alive = readerAlive.get(sessionId);
        if (alive != null && !alive) {
            // 读取线程已退出，检查 channel 是否还活着
            ChannelShell channel = channels.get(sessionId);
            if (channel == null || !channel.isConnected()) {
                // Channel 已断开，返回断开标记让前端感知
                return "\u001b[31m\r\n[连接已断开]\u001b[0m\r\n";
            }
            // Channel 还在但线程退了（罕见），尝试重启读取线程
            InputStream in = inputStreams.get(sessionId);
            if (in != null) {
                log.info("尝试重启终端读取线程 sessionId={}", sessionId);
                startOutputReader(sessionId, in);
            }
        }

        // 智能等待：最多等待 500ms，有数据时额外等 50ms 让数据积累
        long deadline = System.currentTimeMillis() + 500;
        try {
            while (System.currentTimeMillis() < deadline) {
                synchronized (buffer) {
                    if (buffer.length() > 0) {
                        // 已有数据，等待 50ms 积累更多
                        TimeUnit.MILLISECONDS.sleep(50);
                        String output = buffer.toString();
                        buffer.setLength(0);
                        return output;
                    }
                }

                // 检查读取线程是否仍在运行
                alive = readerAlive.get(sessionId);
                if (alive != null && !alive) {
                    // 线程刚退出，返回空（下次 read 会走上面的断开检测）
                    return "";
                }

                TimeUnit.MILLISECONDS.sleep(20);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        // 超时，返回当前缓冲区内容
        synchronized (buffer) {
            String output = buffer.toString();
            buffer.setLength(0);
            return output;
        }
    }

    @Override
    public void resize(String sessionId, int cols, int rows) {
        ChannelShell channel = channels.get(sessionId);
        if (channel == null || !channel.isConnected()) {
            throw new IllegalArgumentException("终端会话不存在或已关闭 sessionId=" + sessionId);
        }

        try {
            channel.setPtySize(cols, rows, 480, 640);
            log.debug("终端大小已调整 sessionId={} {}x{}", sessionId, cols, rows);
        } catch (Exception e) {
            log.error("调整终端大小失败 sessionId={}", sessionId, e);
            throw new RuntimeException("调整终端大小失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void closeSession(String sessionId) {
        log.info("关闭终端会话 sessionId={}", sessionId);
        cleanup(sessionId);
    }

    @Override
    public boolean sessionExists(String sessionId) {
        ChannelShell channel = channels.get(sessionId);
        return channel != null && channel.isConnected();
    }

    // ========== 内部方法 ==========

    /**
     * 启动输出读取线程
     */
    private void startOutputReader(String sessionId, InputStream in) {
        readerAlive.put(sessionId, true);
        Thread reader = new Thread(() -> {
            byte[] buf = new byte[4096];
            try {
                int len;
                while ((len = in.read(buf)) != -1) {
                    String text = new String(buf, 0, len, StandardCharsets.UTF_8);
                    StringBuilder buffer = outputBuffers.get(sessionId);
                    if (buffer != null) {
                        synchronized (buffer) {
                            buffer.append(text);
                        }
                    }
                }
                // in.read() 返回 -1，说明 shell channel EOF
                log.warn("终端 Shell Channel EOF sessionId={}", sessionId);
            } catch (IOException e) {
                log.debug("终端输出读取异常 sessionId={} reason={}", sessionId, e.getMessage());
            } finally {
                readerAlive.put(sessionId, false);

                // 诊断：为什么线程退出了？
                ChannelShell ch = channels.get(sessionId);
                boolean channelConnected = ch != null && ch.isConnected();
                boolean channelClosed = ch != null && ch.isClosed();
                log.warn("终端输出读取线程退出 sessionId={} channelConnected={} channelClosed={}",
                        sessionId, channelConnected, channelClosed);

                // 通知等待中的 read() 方法：线程已退出，不再有数据
                StringBuilder buffer = outputBuffers.get(sessionId);
                if (buffer != null) {
                    synchronized (buffer) {
                        buffer.notifyAll();
                    }
                }
            }
        }, "terminal-reader-" + sessionId);
        reader.setDaemon(true);
        reader.start();
    }

    /**
     * 清理资源
     */
    private void cleanup(String sessionId) {
        try {
            OutputStream out = outputStreams.remove(sessionId);
            if (out != null) out.close();
        } catch (IOException ignored) {}

        try {
            InputStream in = inputStreams.remove(sessionId);
            if (in != null) in.close();
        } catch (IOException ignored) {}

        ChannelShell channel = channels.remove(sessionId);
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }

        outputBuffers.remove(sessionId);
    }

}
