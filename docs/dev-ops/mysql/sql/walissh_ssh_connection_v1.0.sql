-- =============================================
-- WaLiSSH SSH连接配置 库表设计
-- 数据库: walissh
-- 作者: WaLiSSH Dev Team
-- =============================================

CREATE DATABASE IF NOT EXISTS `walissh` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `walissh`;

-- -------------------------------------------
-- 1. SSH连接配置表
-- -------------------------------------------
DROP TABLE IF EXISTS `ssh_connection`;
CREATE TABLE `ssh_connection` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `connection_id` VARCHAR(64) NOT NULL COMMENT '连接唯一标识(UUID)',
    `connection_name` VARCHAR(128) NOT NULL COMMENT '连接名称',
    `host` VARCHAR(255) NOT NULL COMMENT '主机地址',
    `port` INT NOT NULL DEFAULT 22 COMMENT '端口号',
    `username` VARCHAR(128) NOT NULL COMMENT '用户名',
    `auth_type` TINYINT NOT NULL DEFAULT 1 COMMENT '认证类型:1-密码,2-私钥',
    `password` VARCHAR(512) DEFAULT NULL COMMENT '密码(加密存储)',
    `private_key` LONGTEXT DEFAULT NULL COMMENT '私钥内容(加密存储)',
    `encrypted` TINYINT NOT NULL DEFAULT 1 COMMENT '是否加密:0-否,1-是',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '连接状态:0-未连接,1-已连接,2-连接中,3-连接失败',
    `user_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '用户ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0-未删除,1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_connection_id` (`connection_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SSH连接配置表';

-- -------------------------------------------
-- 2. SSH连接高级配置表
-- -------------------------------------------
DROP TABLE IF EXISTS `ssh_connection_config`;
CREATE TABLE `ssh_connection_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `connection_id` VARCHAR(64) NOT NULL COMMENT '关联的连接ID',
    `connect_timeout` INT NOT NULL DEFAULT 10 COMMENT '连接超时时间(秒)',
    `keepalive_interval` INT NOT NULL DEFAULT 60 COMMENT '保活间隔(秒)',
    `startup_command` VARCHAR(512) DEFAULT NULL COMMENT '连接后执行的启动命令',
    `compression` TINYINT NOT NULL DEFAULT 0 COMMENT '是否压缩:0-否,1-是',
    `strict_host_key_check` TINYINT NOT NULL DEFAULT 1 COMMENT '严格主机密钥检查:0-否,1-是',
    `known_hosts` LONGTEXT DEFAULT NULL COMMENT '已知主机密钥列表',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_connection_id` (`connection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SSH连接高级配置表';

-- -------------------------------------------
-- 3. SSH连接会话记录表(可选,用于审计)
-- -------------------------------------------
DROP TABLE IF EXISTS `ssh_session_log`;
CREATE TABLE `ssh_session_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话唯一标识(UUID)',
    `connection_id` VARCHAR(64) NOT NULL COMMENT '关联的连接ID',
    `user_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '用户ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '会话状态:0-打开,1-关闭',
    `remote_addr` VARCHAR(64) DEFAULT NULL COMMENT '远程地址',
    `start_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '会话开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '会话结束时间',
    `error_msg` VARCHAR(512) DEFAULT NULL COMMENT '错误信息',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`),
    KEY `idx_connection_id` (`connection_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SSH会话记录表';
