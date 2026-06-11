package cn.stackssh.infrastructure.adapter.port;

import cn.stackssh.domain.knowledge.adapter.port.IVectorStorePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 自定义向量库适配器
 * 优先使用 OpenAI-compatible EmbeddingModel 做向量相似度检索；
 * 若 embedding API 不可用（如 DeepSeek 不支持 embeddings），自动降级为关键词 TF 匹配。
 */
@Slf4j
@Component
public class VectorStoreAdapter implements IVectorStorePort {

    private final EmbeddingModel embeddingModel;
    private final String persistPath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<VectorEntry> store = new CopyOnWriteArrayList<>();

    public VectorStoreAdapter(
            @Qualifier("knowledgeEmbeddingModel") EmbeddingModel embeddingModel,
            @Value("${knowledge.vector-store.persist-path}") String persistPath) {
        this.embeddingModel = embeddingModel;
        this.persistPath = persistPath;
    }

    @PostConstruct
    public void loadFromDisk() {
        File file = new File(persistPath);
        if (!file.exists()) return;
        try {
            VectorEntry[] entries = objectMapper.readValue(file, VectorEntry[].class);
            store.addAll(Arrays.asList(entries));
            log.info("向量库从文件恢复 {} 条记录: {}", store.size(), persistPath);
        } catch (Exception e) {
            log.warn("向量库文件加载失败，使用空库: {}", e.getMessage());
        }
    }

    @Override
    public void addDocuments(List<VectorDocument> documents) {
        int success = 0, fallback = 0;
        for (VectorDocument doc : documents) {
            VectorEntry entry = new VectorEntry();
            entry.setContent(doc.getContent());
            entry.setMetadata(doc.getMetadata());
            try {
                float[] vector = embeddingModel.embed(doc.getContent());
                entry.setEmbedding(vector);
                success++;
            } catch (Exception e) {
                // embedding 不可用时存空向量，搜索时降级为关键词匹配
                entry.setEmbedding(new float[0]);
                fallback++;
            }
            store.add(entry);
        }
        if (fallback > 0) {
            log.warn("向量化失败 {}/{} 个 chunk，将使用关键词匹配（embedding API 不支持当前模型）",
                    fallback, documents.size());
        }
        if (success > 0) {
            log.info("向量化成功 {} 个 chunk", success);
        }
        persist();
    }

    @Override
    public void deleteByDocId(String docId) {
        store.removeIf(entry -> docId.equals(entry.getMetadata().get("docId")));
        persist();
    }

    @Override
    public List<String> search(String query, String connectionId, int topK) {
        if (store.isEmpty()) return Collections.emptyList();

        // 按 connectionId 过滤
        List<VectorEntry> candidates = store.stream()
                .filter(entry -> {
                    String docConn = entry.getMetadata().getOrDefault("connectionId", "");
                    return docConn.isEmpty()
                            || connectionId == null || connectionId.isEmpty()
                            || docConn.equals(connectionId);
                })
                .collect(Collectors.toList());

        if (candidates.isEmpty()) return Collections.emptyList();

        // 判断是否有有效向量（非空 embedding）
        boolean hasVectors = candidates.stream().anyMatch(e -> e.getEmbedding() != null && e.getEmbedding().length > 0);

        if (hasVectors) {
            // 向量相似度模式
            try {
                float[] queryVec = embeddingModel.embed(query);
                return candidates.stream()
                        .map(entry -> {
                            float score = (entry.getEmbedding() != null && entry.getEmbedding().length > 0)
                                    ? cosineSimilarity(queryVec, entry.getEmbedding())
                                    : keywordScore(query, entry.getContent());
                            return new ScoredEntry(entry, score);
                        })
                        .sorted(Comparator.comparingDouble(ScoredEntry::getScore).reversed())
                        .limit(topK)
                        .map(se -> se.getEntry().getContent())
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.warn("查询向量化失败，降级为关键词匹配: {}", e.getMessage());
            }
        }

        // 关键词匹配模式（embedding 不可用时的兜底）
        return candidates.stream()
                .map(entry -> new ScoredEntry(entry, keywordScore(query, entry.getContent())))
                .filter(se -> se.getScore() > 0)
                .sorted(Comparator.comparingDouble(ScoredEntry::getScore).reversed())
                .limit(topK)
                .map(se -> se.getEntry().getContent())
                .collect(Collectors.toList());
    }

    // ── 相似度计算 ────────────────────────────────────────────────

    private float cosineSimilarity(float[] a, float[] b) {
        float dot = 0, normA = 0, normB = 0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0f : (float) (dot / denom);
    }

    /**
     * 关键词匹配分数：先按空格/标点拆词；若无命中（典型中文无空格），降级为 bigram 匹配。
     */
    private float keywordScore(String query, String content) {
        String lowerContent = content.toLowerCase();
        String lowerQuery = query.toLowerCase();

        // 1. 词语级匹配（英文 / 含空格的短语）
        String[] terms = lowerQuery.split("[\\s，。！？,!?\\.；;：:、]+");
        int termHits = 0;
        for (String term : terms) {
            if (term.length() >= 2 && lowerContent.contains(term)) termHits++;
        }
        if (termHits > 0) return (float) termHits / terms.length;

        // 2. Bigram 匹配（中文无空格场景兜底）
        int bigramTotal = 0, bigramHits = 0;
        for (int i = 0; i < lowerQuery.length() - 1; i++) {
            String bigram = lowerQuery.substring(i, i + 2);
            if (bigram.chars().anyMatch(c -> Character.getType(c) == Character.OTHER_LETTER)) {
                bigramTotal++;
                if (lowerContent.contains(bigram)) bigramHits++;
            }
        }
        return bigramTotal > 0 ? (float) bigramHits / bigramTotal : 0;
    }

    private void persist() {
        try {
            File file = new File(persistPath);
            file.getParentFile().mkdirs();
            objectMapper.writeValue(file, store);
        } catch (Exception e) {
            log.warn("向量库持久化失败: {}", e.getMessage());
        }
    }

    // ── 内部数据结构 ──────────────────────────────────────────────

    public static class VectorEntry {
        private String content;
        private float[] embedding;
        private Map<String, String> metadata = new HashMap<>();

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public float[] getEmbedding() { return embedding; }
        public void setEmbedding(float[] embedding) { this.embedding = embedding; }
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    }

    private static class ScoredEntry {
        private final VectorEntry entry;
        private final float score;

        ScoredEntry(VectorEntry entry, float score) {
            this.entry = entry;
            this.score = score;
        }

        VectorEntry getEntry() { return entry; }
        float getScore() { return score; }
    }

}
