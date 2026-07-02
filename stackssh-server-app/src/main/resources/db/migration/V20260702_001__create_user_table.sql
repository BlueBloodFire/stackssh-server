CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` VARCHAR(64) NOT NULL COMMENT 'user unique id',
  `username` VARCHAR(64) NOT NULL COMMENT 'username',
  `password` VARCHAR(128) NOT NULL COMMENT 'bcrypt password hash',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0=disabled, 1=enabled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_user_id` (`user_id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='system user';
