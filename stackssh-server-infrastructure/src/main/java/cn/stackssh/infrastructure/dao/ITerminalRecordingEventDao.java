package cn.stackssh.infrastructure.dao;

import cn.stackssh.infrastructure.dao.po.TerminalRecordingEventPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ITerminalRecordingEventDao {
    void batchInsert(@Param("list") List<TerminalRecordingEventPO> list);
    List<TerminalRecordingEventPO> queryByRecordingDbId(@Param("recordingDbId") long recordingDbId);
}
