-- =============================================
-- WaLiSSH 会话持久化 库表设计 (Phase 5)
-- 数据库: walissh
-- =============================================

USE `walissh`;

-- 会话元数据
CREATE TABLE IF NOT EXISTS `chat_session` (
    `id`            VARCHAR(64)     NOT NULL COMMENT '会话ID',
    `agent_id`      VARCHAR(64)     NOT NULL COMMENT '智能体ID',
    `user_id`       VARCHAR(64)     NOT NULL COMMENT '用户ID',
    `title`         VARCHAR(200)    DEFAULT NULL COMMENT '会话标题',
    `created_at`    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `message_count` INT             DEFAULT 0 COMMENT '消息数量',
    PRIMARY KEY (`id`),
    INDEX `idx_user_agent` (`user_id`, `agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话会话';

-- 对话消息
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `session_id`    VARCHAR(64)     NOT NULL COMMENT '会话ID',
    `role`          VARCHAR(20)     NOT NULL COMMENT '角色: user/assistant/tool/system',
    `content`       TEXT            COMMENT '消息内容',
    `tool_name`     VARCHAR(100)    DEFAULT NULL COMMENT '工具名称',
    `tool_call_id`  VARCHAR(100)    DEFAULT NULL COMMENT '工具调用ID',
    `priority`      VARCHAR(20)     DEFAULT 'MEDIUM' COMMENT '优先级: CRITICAL/HIGH/MEDIUM/LOW',
    `token_count`   INT             DEFAULT 0 COMMENT '预估 token 数',
    `created_at`    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_session_time` (`session_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息';

-- 里程碑事件
CREATE TABLE IF NOT EXISTS `chat_milestone` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `session_id`    VARCHAR(64)     NOT NULL COMMENT '会话ID',
    `type`          VARCHAR(30)     NOT NULL COMMENT '类型: TASK_CHANGE/ERROR/DECISION/...',
    `content`       TEXT            COMMENT '内容摘要',
    `created_at`    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_session_time` (`session_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话里程碑';
