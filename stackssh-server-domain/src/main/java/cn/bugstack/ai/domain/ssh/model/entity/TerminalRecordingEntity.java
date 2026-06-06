package cn.bugstack.ai.domain.ssh.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 终端录制聚合实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TerminalRecordingEntity {

    private Long id;
    private String recordingId;
    private String connectionId;
    private String sessionId;
    private Integer cols;
    private Integer rows;
    /** 0=录制中 1=已完成 2=已中断 */
    private Integer status;
    private Date startedAt;
    private Date endedAt;
    private Long durationMs;

    @Builder.Default
    private List<Event> events = new ArrayList<>();

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Event {
        private Long offsetMs;
        private String data;
    }
}
