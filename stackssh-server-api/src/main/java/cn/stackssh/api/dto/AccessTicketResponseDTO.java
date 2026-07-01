package cn.stackssh.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessTicketResponseDTO {
    private String ticket;
    private Long expiresInSeconds;
}
