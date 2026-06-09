package cn.stackssh.domain.ssh.service;

import cn.stackssh.domain.ssh.model.entity.TerminalSessionEntity;

/**
 * SSH终端领域服务接口
 * 定义终端会话的核心业务操作
 *
 * @author waissh dev
 */
public interface ISshTerminalService {

    /**
     * 打开终端会话
     *
     * @param connectionId SSH连接ID
     * @param cols          终端列数
     * @param rows          终端行数
     * @return 终端会话实体
     */
    TerminalSessionEntity openTerminal(String connectionId, int cols, int rows);

    /**
     * 执行命令并返回输出
     *
     * @param sessionId 会话ID
     * @param command    命令内容
     * @return 命令执行后的终端输出
     */
    String executeCommand(String sessionId, String command);

    /**
     * 调整终端大小
     *
     * @param sessionId 会话ID
     * @param cols       新的列数
     * @param rows       新的行数
     */
    void resizeTerminal(String sessionId, int cols, int rows);

    /**
     * 获取终端会话
     *
     * @param sessionId 会话ID
     * @return 终端会话实体
     */
    TerminalSessionEntity getTerminalSession(String sessionId);

    /**
     * 关闭终端会话
     *
     * @param sessionId 会话ID
     */
    void closeTerminal(String sessionId);

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话ID
     * @return 是否存在
     */
    boolean sessionExists(String sessionId);

    /**
     * 读取终端当前输出（不执行命令，用于同步状态）
     *
     * @param sessionId 会话ID
     * @return 当前终端输出
     */
    String readTerminal(String sessionId);

    /**
     * 写入原始输入到终端（逐字节模式，由 Shell 自身处理 echo）
     *
     * @param sessionId 会话ID
     * @param input     原始输入数据
     */
    void writeTerminal(String sessionId, String input);

    /**
     * 开启终端录制
     *
     * @param sessionId    会话ID
     * @param connectionId 连接ID（用于元信息）
     * @return 录制ID
     */
    String startRecording(String sessionId, String connectionId);

    /**
     * 停止录制并持久化
     *
     * @param sessionId 会话ID
     * @param recordingId 录制ID
     */
    void stopRecording(String sessionId, String recordingId);

    /**
     * 关闭某个 SSH 连接下的所有终端 session（SSH 断开时调用，防止 cache 残留）
     *
     * @param connectionId SSH 连接ID
     */
    void closeTerminalsByConnection(String connectionId);

    /**
     * 获取连接的录制列表
     */
    java.util.List<cn.stackssh.domain.ssh.model.entity.TerminalRecordingEntity> listRecordings(String connectionId);

    /**
     * 获取录制详情（含所有事件，用于回放）
     */
    cn.stackssh.domain.ssh.model.entity.TerminalRecordingEntity getRecordingPlayback(String recordingId);

}