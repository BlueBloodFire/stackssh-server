ALTER TABLE `chat_session_state`
  ADD COLUMN `last_enhancement_cache_hit` tinyint(1) DEFAULT NULL COMMENT '最近一次增强缓存是否命中' AFTER `cached_rag_chunks`,
  ADD COLUMN `last_enhancement_cache_reason` varchar(64) DEFAULT NULL COMMENT '最近一次增强缓存命中/未命中原因' AFTER `last_enhancement_cache_hit`;
