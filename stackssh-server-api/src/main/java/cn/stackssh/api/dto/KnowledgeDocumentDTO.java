package cn.stackssh.api.dto;

import lombok.Data;

@Data
public class KnowledgeDocumentDTO {

    private String docId;
    private String connectionId;
    private String agentId;
    private String name;
    private String fileType;
    /** 0=处理中 1=就绪 2=失败 */
    private Integer status;
    private Integer chunkCount;
    private String createdAt;

}
