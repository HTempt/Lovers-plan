-- 情侣足迹地图
-- V005: footprint + diary location detail fields

-- 足迹表
CREATE TABLE IF NOT EXISTS `footprint` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL COMMENT '情侣关系ID',
    `diary_id` BIGINT NOT NULL COMMENT '关联日记ID',
    `province` VARCHAR(50) DEFAULT '' COMMENT '省份',
    `city` VARCHAR(50) DEFAULT '' COMMENT '城市',
    `location_name` VARCHAR(200) DEFAULT '' COMMENT '地点名称',
    `latitude` DECIMAL(10,7) DEFAULT 0 COMMENT '纬度',
    `longitude` DECIMAL(10,7) DEFAULT 0 COMMENT '经度',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_couple_id (`couple_id`),
    INDEX idx_diary_id (`diary_id`),
    INDEX idx_city (`city`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情侣足迹表';

-- 日记表增加位置详情字段
ALTER TABLE `diary`
    ADD COLUMN `province` VARCHAR(50) DEFAULT '' COMMENT '省份' AFTER `location`,
    ADD COLUMN `city` VARCHAR(50) DEFAULT '' COMMENT '城市' AFTER `province`,
    ADD COLUMN `latitude` DECIMAL(10,7) DEFAULT 0 COMMENT '纬度' AFTER `city`,
    ADD COLUMN `longitude` DECIMAL(10,7) DEFAULT 0 COMMENT '经度' AFTER `latitude`;
