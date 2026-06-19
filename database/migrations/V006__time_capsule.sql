-- 时光胶囊
-- V006: time_capsule + time_capsule_media

-- 时光胶囊表
CREATE TABLE IF NOT EXISTS `time_capsule` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL COMMENT '情侣关系ID',
    `user_id` BIGINT NOT NULL COMMENT '创建者用户ID',
    `type` VARCHAR(30) NOT NULL COMMENT '胶囊类型 to_future_ta/to_future_us/birthday/anniversary/wish',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '文字内容',
    `open_at` DATETIME NOT NULL COMMENT '预定开启时间',
    `opened_at` DATETIME DEFAULT NULL COMMENT '实际开启时间',
    `status` TINYINT DEFAULT 0 COMMENT '状态 0=DRAFT(草稿) 1=SEALED(已封存) 2=OPENABLE(可开启) 3=OPENED(已开启)',
    `pair_id` BIGINT DEFAULT NULL COMMENT '关联胶囊ID（双人模式）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_couple_id (`couple_id`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_status (`status`),
    INDEX idx_open_at (`open_at`),
    INDEX idx_pair_id (`pair_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时光胶囊表';

-- 胶囊媒体表
CREATE TABLE IF NOT EXISTS `time_capsule_media` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `capsule_id` BIGINT NOT NULL COMMENT '胶囊ID',
    `media_type` VARCHAR(20) NOT NULL COMMENT '媒体类型 image/video/audio',
    `file_url` VARCHAR(1000) NOT NULL COMMENT '文件URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_capsule_id (`capsule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='胶囊媒体表';
