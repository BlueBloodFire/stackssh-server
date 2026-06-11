package cn.stackssh.api.dto;

import lombok.Data;

@Data
public class KnowledgeSearchRequestDTO {

    private String query;
    private String connectionId;
    private Integer topK;

}
