CREATE TABLE knowledge_document (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  doc_id VARCHAR(64) NOT NULL UNIQUE COMMENT '文档唯一ID',
  connection_id VARCHAR(64) DEFAULT NULL COMMENT '关联服务器，NULL表示全局',
  agent_id VARCHAR(64) DEFAULT NULL COMMENT '关联Agent',
  name VARCHAR(255) NOT NULL COMMENT '文件名',
  file_type VARCHAR(32) NOT NULL COMMENT 'txt/md/pdf',
  status TINYINT DEFAULT 0 COMMENT '0=处理中 1=就绪 2=失败',
  chunk_count INT DEFAULT 0 COMMENT '切块数量',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_connection_id (connection_id),
  INDEX idx_agent_id (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG知识库文档元数据';
