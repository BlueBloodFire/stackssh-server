package cn.bugstack.ai.domain.ssh.adapter.port;

import cn.bugstack.ai.domain.ssh.model.entity.TerminalRecordingEntity;

import java.util.List;

/**
 * 终端录制存储端口接口
 */
public interface ITerminalRecordingPort {

    /** 新建录制记录，返回 DB 主键 */
    long createRecording(String recordingId, String connectionId, String sessionId, int cols, int rows);

    /** 更新录制状态（完成/中断）*/
    void finishRecording(String recordingId, int status, long durationMs);

    /** 批量持久化录制事件 */
    void saveEvents(long recordingDbId, List<TerminalRecordingEntity.Event> events);

    /** 查询连接的录制列表 */
    List<TerminalRecordingEntity> listByConnectionId(String connectionId);

    /** 查询录制详情（含事件） */
    TerminalRecordingEntity getRecordingWithEvents(String recordingId);
}
