package cn.stackssh.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class ConversationRoundTraceResponseDTO {

    private String sessionId;
    private List<ConversationRoundTraceItemDTO> items;
}
