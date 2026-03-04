-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` VARCHAR(64) PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密后）',
    `email` VARCHAR(100) COMMENT '邮箱',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 数据库配置表
CREATE TABLE IF NOT EXISTS `database_config` (
    `id` VARCHAR(64) PRIMARY KEY,
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '配置名称',
    `type` VARCHAR(20) NOT NULL COMMENT '数据库类型',
    `host` VARCHAR(100) NOT NULL COMMENT '主机地址',
    `port` INT NOT NULL COMMENT '端口',
    `database_name` VARCHAR(100) NOT NULL COMMENT '数据库名',
    `username` VARCHAR(100) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库配置表';

-- 查询模板表
CREATE TABLE IF NOT EXISTS `query_template` (
    `id` VARCHAR(64) PRIMARY KEY,
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `description` VARCHAR(500) COMMENT '模板描述',
    `query_example` TEXT COMMENT '查询示例',
    `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否默认模板',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查询模板表';

-- 知识库表
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` VARCHAR(64) PRIMARY KEY,
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `type` VARCHAR(20) NOT NULL COMMENT '类型：SQL_EXAMPLE/GENERAL_DOC/BUSINESS_RULE/TERM_MAPPING',
    `domain` VARCHAR(50) COMMENT '领域：预算执行/工程投资/自然资源（可空）',
    `question` TEXT COMMENT '提问（仅SQL_EXAMPLE类型使用）',
    `chunk_index` INT NOT NULL COMMENT '分片编号，从0开始，自动生成',
    `content` TEXT NOT NULL COMMENT '分片内容（SQL语句或文档内容）',
    `embedding_id` VARCHAR(100) COMMENT '向量存储中的ID，用于增量更新',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_type` (`user_id`, `type`),
    INDEX `idx_user_domain_type` (`user_id`, `domain`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';
