package cn.bugstack.ai.infrastructure.dao;

import cn.bugstack.ai.infrastructure.dao.po.TerminalRecordingEventPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ITerminalRecordingEventDao {
    void batchInsert(@Param("list") List<TerminalRecordingEventPO> list);
    List<TerminalRecordingEventPO> queryByRecordingDbId(@Param("recordingDbId") long recordingDbId);
}
