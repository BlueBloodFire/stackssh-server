package cn.stackssh.domain.knowledge.adapter.port;

import java.util.List;
import java.util.Map;

public interface IVectorStorePort {

    void addDocuments(List<VectorDocument> documents);

    void deleteByDocId(String docId);

    List<String> search(String query, String connectionId, int topK);

    class VectorDocument {
        private String content;
        private Map<String, String> metadata;

        public VectorDocument(String content, Map<String, String> metadata) {
            this.content = content;
            this.metadata = metadata;
        }

        public String getContent() { return content; }
        public Map<String, String> getMetadata() { return metadata; }
    }
}
