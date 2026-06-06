package cn.bugstack.ai.infrastructure.dao;

import cn.bugstack.ai.infrastructure.dao.po.TerminalRecordingPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ITerminalRecordingDao {
    void insert(TerminalRecordingPO po);
    void updateFinished(@Param("recordingId") String recordingId,
                        @Param("status") int status,
                        @Param("durationMs") long durationMs);
    TerminalRecordingPO queryByRecordingId(@Param("recordingId") String recordingId);
    List<TerminalRecordingPO> queryByConnectionId(@Param("connectionId") String connectionId);
}
