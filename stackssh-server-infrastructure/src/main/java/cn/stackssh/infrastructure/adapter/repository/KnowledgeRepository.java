package cn.stackssh.infrastructure.adapter.repository;

import cn.stackssh.domain.knowledge.adapter.repository.IKnowledgeRepository;
import cn.stackssh.domain.knowledge.model.entity.KnowledgeDocumentEntity;
import cn.stackssh.infrastructure.dao.IKnowledgeDocumentDao;
import cn.stackssh.infrastructure.dao.po.KnowledgeDocumentPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class KnowledgeRepository implements IKnowledgeRepository {

    @Resource
    private IKnowledgeDocumentDao knowledgeDocumentDao;

    @Override
    public void saveDocument(KnowledgeDocumentEntity entity) {
        KnowledgeDocumentPO po = toPoFromEntity(entity);
        knowledgeDocumentDao.insert(po);
    }

    @Override
    public void updateStatus(String docId, int status, int chunkCount) {
        knowledgeDocumentDao.updateStatus(docId, status, chunkCount);
    }

    @Override
    public void deleteDocument(String docId) {
        knowledgeDocumentDao.delete(docId);
    }

    @Override
    public KnowledgeDocumentEntity queryByDocId(String docId) {
        KnowledgeDocumentPO po = knowledgeDocumentDao.queryByDocId(docId);
        return po == null ? null : toEntity(po);
    }

    @Override
    public List<KnowledgeDocumentEntity> queryList(String connectionId, String agentId) {
        return knowledgeDocumentDao.queryList(connectionId, agentId)
                .stream().map(this::toEntity).collect(Collectors.toList());
    }

    private KnowledgeDocumentPO toPoFromEntity(KnowledgeDocumentEntity e) {
        KnowledgeDocumentPO po = new KnowledgeDocumentPO();
        po.setDocId(e.getDocId());
        po.setConnectionId(e.getConnectionId());
        po.setAgentId(e.getAgentId());
        po.setName(e.getName());
        po.setFileType(e.getFileType());
        po.setStatus(e.getStatus());
        po.setChunkCount(e.getChunkCount());
        return po;
    }

    private KnowledgeDocumentEntity toEntity(KnowledgeDocumentPO po) {
        String createdAt = po.getCreatedAt() != null
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(po.getCreatedAt()) : null;
        return KnowledgeDocumentEntity.builder()
                .id(po.getId())
                .docId(po.getDocId())
                .connectionId(po.getConnectionId())
                .agentId(po.getAgentId())
                .name(po.getName())
                .fileType(po.getFileType())
                .status(po.getStatus())
                .chunkCount(po.getChunkCount())
                .createdAt(createdAt)
                .build();
    }

}
