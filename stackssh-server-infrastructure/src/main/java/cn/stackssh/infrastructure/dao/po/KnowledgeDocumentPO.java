package cn.stackssh.infrastructure.dao.po;

import lombok.Data;

import java.util.Date;

@Data
public class KnowledgeDocumentPO {

    private Long id;
    private String docId;
    private String connectionId;
    private String agentId;
    private String name;
    private String fileType;
    private Integer status;
    private Integer chunkCount;
    private Date createdAt;

}
