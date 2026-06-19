-- 爱情树成长系统
-- V004: love_tree + love_tree_growth_record + sign_in_record

-- 爱情树表（情侣共享一棵树）
CREATE TABLE IF NOT EXISTS `love_tree` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL COMMENT '情侣关系ID',
    `level` INT DEFAULT 1 COMMENT '当前等级 Lv1~Lv7',
    `growth_value` INT DEFAULT 0 COMMENT '当前成长值',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_couple_id (`couple_id`),
    INDEX idx_couple_id (`couple_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爱情树表';

-- 成长记录表
CREATE TABLE IF NOT EXISTS `love_tree_growth_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL COMMENT '情侣关系ID',
    `action_type` VARCHAR(30) NOT NULL COMMENT '行为类型: diary/task/wish/sign_in/status',
    `growth_value` INT NOT NULL DEFAULT 0 COMMENT '本次获得的成长值',
    `source_id` BIGINT DEFAULT NULL COMMENT '关联的业务记录ID',
    `description` VARCHAR(200) DEFAULT '' COMMENT '行为描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_couple_id (`couple_id`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爱情树成长记录表';

-- 每日签到表
CREATE TABLE IF NOT EXISTS `sign_in_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `couple_id` BIGINT NOT NULL COMMENT '情侣关系ID',
    `sign_date` DATE NOT NULL COMMENT '签到日期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (`user_id`, `sign_date`),
    INDEX idx_couple_id (`couple_id`),
    INDEX idx_sign_date (`sign_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日签到表';
