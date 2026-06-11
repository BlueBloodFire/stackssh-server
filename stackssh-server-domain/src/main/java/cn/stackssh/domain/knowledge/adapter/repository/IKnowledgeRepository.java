package cn.stackssh.domain.knowledge.adapter.repository;

import cn.stackssh.domain.knowledge.model.entity.KnowledgeDocumentEntity;

import java.util.List;

public interface IKnowledgeRepository {

    void saveDocument(KnowledgeDocumentEntity entity);

    void updateStatus(String docId, int status, int chunkCount);

    void deleteDocument(String docId);

    KnowledgeDocumentEntity queryByDocId(String docId);

    List<KnowledgeDocumentEntity> queryList(String connectionId, String agentId);

}
