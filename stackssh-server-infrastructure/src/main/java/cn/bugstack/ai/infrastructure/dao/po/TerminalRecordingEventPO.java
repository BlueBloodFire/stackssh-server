package cn.bugstack.ai.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TerminalRecordingEventPO {
    private Long id;
    private Long recordingDbId;
    /** 距录制开始的毫秒偏移 */
    private Long offsetMs;
    /** 终端输出内容（base64 编码，保留控制字符） */
    private String data;
}
