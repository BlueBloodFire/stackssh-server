package cn.stackssh.trigger.http;

import cn.stackssh.api.dto.RecordingStartRequestDTO;
import cn.stackssh.api.dto.RecordingStopRequestDTO;
import cn.stackssh.api.response.Response;
import cn.stackssh.domain.ssh.model.entity.TerminalRecordingEntity;
import cn.stackssh.domain.ssh.service.ISshTerminalService;
import cn.stackssh.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 终端录制/回放 HTTP 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ssh/terminal/recording")
@CrossOrigin(origins = "*")
public class SshTerminalRecordingController {

    @Resource
    private ISshTerminalService sshTerminalService;

    /** 开始录制 */
    @PostMapping("/start")
    public Response<String> startRecording(@RequestBody RecordingStartRequestDTO dto) {
        try {
            String recordingId = sshTerminalService.startRecording(dto.getSessionId(), dto.getConnectionId());
            return Response.<String>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(recordingId)
                    .build();
        } catch (IllegalArgumentException e) {
            log.warn("开始录制失败: {}", e.getMessage());
            return Response.<String>builder()
                    .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                    .info(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("开始录制异常", e);
            return Response.<String>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("开始录制失败: " + e.getMessage())
                    .build();
        }
    }

    /** 停止录制 */
    @PostMapping("/stop")
    public Response<Void> stopRecording(@RequestBody RecordingStopRequestDTO dto) {
        try {
            sshTerminalService.stopRecording(dto.getSessionId(), dto.getRecordingId());
            return Response.<Void>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .build();
        } catch (IllegalArgumentException e) {
            log.warn("停止录制失败: {}", e.getMessage());
            return Response.<Void>builder()
                    .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                    .info(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("停止录制异常", e);
            return Response.<Void>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("停止录制失败: " + e.getMessage())
                    .build();
        }
    }

    /** 查询连接的录制列表 */
    @GetMapping("/list")
    public Response<List<TerminalRecordingEntity>> listRecordings(@RequestParam("connectionId") String connectionId) {
        try {
            List<TerminalRecordingEntity> list = sshTerminalService.listRecordings(connectionId);
            return Response.<List<TerminalRecordingEntity>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(list)
                    .build();
        } catch (Exception e) {
            log.error("查询录制列表失败", e);
            return Response.<List<TerminalRecordingEntity>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("查询录制列表失败: " + e.getMessage())
                    .build();
        }
    }

    /** 获取录制回放数据（含所有事件） */
    @GetMapping("/playback/{recordingId}")
    public Response<TerminalRecordingEntity> getPlayback(@PathVariable("recordingId") String recordingId) {
        try {
            TerminalRecordingEntity entity = sshTerminalService.getRecordingPlayback(recordingId);
            if (entity == null) {
                return Response.<TerminalRecordingEntity>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("录制不存在")
                        .build();
            }
            return Response.<TerminalRecordingEntity>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(entity)
                    .build();
        } catch (Exception e) {
            log.error("获取录制回放数据失败", e);
            return Response.<TerminalRecordingEntity>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("获取回放数据失败: " + e.getMessage())
                    .build();
        }
    }
}
