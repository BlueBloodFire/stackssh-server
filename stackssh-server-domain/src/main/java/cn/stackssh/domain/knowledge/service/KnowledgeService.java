package cn.stackssh.domain.knowledge.service;

import cn.stackssh.domain.knowledge.adapter.port.IVectorStorePort;
import cn.stackssh.domain.knowledge.adapter.repository.IKnowledgeRepository;
import cn.stackssh.domain.knowledge.model.entity.KnowledgeDocumentEntity;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class KnowledgeService implements IKnowledgeService {

    @Resource
    private IKnowledgeRepository knowledgeRepository;

    @Resource
    private IVectorStorePort vectorStorePort;

    @Override
    public KnowledgeDocumentEntity uploadDocument(MultipartFile file, String connectionId, String agentId) {
        String docId = UUID.randomUUID().toString().replace("-", "");
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        String fileType = getFileType(fileName);

        KnowledgeDocumentEntity entity = KnowledgeDocumentEntity.builder()
                .docId(docId)
                .connectionId(connectionId)
                .agentId(agentId)
                .name(fileName)
                .fileType(fileType)
                .status(0)
                .chunkCount(0)
                .build();
        knowledgeRepository.saveDocument(entity);

        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<String> chunks = chunkText(text, 500, 50);

            List<IVectorStorePort.VectorDocument> documents = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, String> meta = new HashMap<>();
                meta.put("docId", docId);
                meta.put("connectionId", connectionId != null ? connectionId : "");
                meta.put("agentId", agentId != null ? agentId : "");
                meta.put("chunkIndex", String.valueOf(i));
                meta.put("fileName", fileName);
                documents.add(new IVectorStorePort.VectorDocument(chunks.get(i), meta));
            }

            vectorStorePort.addDocuments(documents);

            knowledgeRepository.updateStatus(docId, 1, chunks.size());
            entity.setStatus(1);
            entity.setChunkCount(chunks.size());
            log.info("知识库文档上传成功 docId={} chunks={}", docId, chunks.size());
        } catch (Exception e) {
            log.error("知识库文档处理失败 docId={}", docId, e);
            knowledgeRepository.updateStatus(docId, 2, 0);
            entity.setStatus(2);
        }

        return entity;
    }

    @Override
    public void deleteDocument(String docId) {
        vectorStorePort.deleteByDocId(docId);
        knowledgeRepository.deleteDocument(docId);
        log.info("知识库文档删除 docId={}", docId);
    }

    @Override
    public List<KnowledgeDocumentEntity> listDocuments(String connectionId, String agentId) {
        return knowledgeRepository.queryList(connectionId, agentId);
    }

    @Override
    public List<String> searchRelevant(String query, String connectionId, int topK) {
        try {
            return vectorStorePort.search(query, connectionId, topK);
        } catch (Exception e) {
            log.warn("知识库检索失败，跳过 RAG 注入: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── 私有工具方法 ──────────────────────────────────────────────

    private List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text.isBlank()) return chunks;
        if (text.length() <= chunkSize) {
            chunks.add(text.trim());
            return chunks;
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            if (end < text.length()) {
                int nl = text.lastIndexOf('\n', end);
                if (nl > start + chunkSize / 2) end = nl + 1;
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) chunks.add(chunk);
            start = end - overlap;
            if (start <= 0 || start >= text.length()) break;
        }
        return chunks;
    }

    private String getFileType(String fileName) {
        if (fileName.endsWith(".md")) return "md";
        if (fileName.endsWith(".pdf")) return "pdf";
        return "txt";
    }

}
