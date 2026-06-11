package cn.stackssh.domain.knowledge.service;

import cn.stackssh.domain.knowledge.model.entity.KnowledgeDocumentEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IKnowledgeService {

    KnowledgeDocumentEntity uploadDocument(MultipartFile file, String connectionId, String agentId);

    void deleteDocument(String docId);

    List<KnowledgeDocumentEntity> listDocuments(String connectionId, String agentId);

    /** 语义检索，返回最相关的 topK 个文本块 */
    List<String> searchRelevant(String query, String connectionId, int topK);

}
