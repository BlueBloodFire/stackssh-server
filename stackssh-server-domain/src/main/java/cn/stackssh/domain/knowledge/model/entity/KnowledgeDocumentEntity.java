package cn.stackssh.domain.knowledge.model.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeDocumentEntity {

    private Long id;
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
