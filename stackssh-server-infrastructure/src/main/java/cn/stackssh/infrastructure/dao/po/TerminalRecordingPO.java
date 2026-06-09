package cn.stackssh.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TerminalRecordingPO {
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
}
