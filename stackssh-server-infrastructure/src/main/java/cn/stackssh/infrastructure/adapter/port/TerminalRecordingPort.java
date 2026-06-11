package cn.stackssh.infrastructure.adapter.port;

import cn.stackssh.domain.ssh.adapter.port.ITerminalRecordingPort;
import cn.stackssh.domain.ssh.model.entity.TerminalRecordingEntity;
import cn.stackssh.infrastructure.dao.ITerminalRecordingDao;
import cn.stackssh.infrastructure.dao.ITerminalRecordingEventDao;
import cn.stackssh.infrastructure.dao.po.TerminalRecordingEventPO;
import cn.stackssh.infrastructure.dao.po.TerminalRecordingPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TerminalRecordingPort implements ITerminalRecordingPort {

    @Resource
    private ITerminalRecordingDao recordingDao;

    @Resource
    private ITerminalRecordingEventDao eventDao;

    @Override
    public long createRecording(String recordingId, String connectionId, String sessionId, int cols, int rows) {
        TerminalRecordingPO po = TerminalRecordingPO.builder()
                .recordingId(recordingId)
                .connectionId(connectionId)
                .sessionId(sessionId)
                .cols(cols)
                .rows(rows)
                .status(0)
                .build();
        recordingDao.insert(po);
        return po.getId();
    }

    @Override
    public void finishRecording(String recordingId, int status, long durationMs) {
        recordingDao.updateFinished(recordingId, status, durationMs);
    }

    @Override
    public void saveEvents(long recordingDbId, List<TerminalRecordingEntity.Event> events) {
        if (events == null || events.isEmpty()) return;
        List<TerminalRecordingEventPO> poList = events.stream()
                .map(e -> TerminalRecordingEventPO.builder()
                        .recordingDbId(recordingDbId)
                        .offsetMs(e.getOffsetMs())
                        .data(e.getData())
                        .build())
                .collect(Collectors.toList());
        eventDao.batchInsert(poList);
    }

    @Override
    public List<TerminalRecordingEntity> listByConnectionId(String connectionId) {
        return recordingDao.queryByConnectionId(connectionId).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public TerminalRecordingEntity getRecordingWithEvents(String recordingId) {
        TerminalRecordingPO po = recordingDao.queryByRecordingId(recordingId);
        if (po == null) return null;
        TerminalRecordingEntity entity = toEntity(po);
        List<TerminalRecordingEventPO> eventPOs = eventDao.queryByRecordingDbId(po.getId());
        List<TerminalRecordingEntity.Event> events = eventPOs.stream()
                .map(e -> TerminalRecordingEntity.Event.builder()
                        .offsetMs(e.getOffsetMs())
                        .data(e.getData())
                        .build())
                .collect(Collectors.toList());
        entity.setEvents(events);
        return entity;
    }

    private TerminalRecordingEntity toEntity(TerminalRecordingPO po) {
        return TerminalRecordingEntity.builder()
                .id(po.getId())
                .recordingId(po.getRecordingId())
                .connectionId(po.getConnectionId())
                .sessionId(po.getSessionId())
                .cols(po.getCols())
                .rows(po.getRows())
                .status(po.getStatus())
                .startedAt(po.getStartedAt())
                .endedAt(po.getEndedAt())
                .durationMs(po.getDurationMs())
                .build();
    }
}
