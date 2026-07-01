package cn.stackssh.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalSessionSummaryDTO {
    private String sessionId;
    private String connectionId;
    private Integer cols;
    private Integer rows;
    private String createdAt;
    private String lastActiveAt;
}
