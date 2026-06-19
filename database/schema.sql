-- 双人岛 数据库建表脚本
-- Database: lovers_plan

CREATE DATABASE IF NOT EXISTS lovers_plan DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE lovers_plan;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `openid` VARCHAR(64) NOT NULL UNIQUE COMMENT '微信openid',
    `nickname` VARCHAR(50) DEFAULT '' COMMENT '昵称',
    `avatar` VARCHAR(500) DEFAULT '' COMMENT '头像URL',
    `gender` TINYINT DEFAULT 0 COMMENT '性别 0未知 1男 2女',
    `couple_id` BIGINT DEFAULT NULL COMMENT '绑定情侣关系ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1正常 0无效',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_openid (`openid`),
    INDEX idx_couple_id (`couple_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 情侣关系表
CREATE TABLE IF NOT EXISTS `couple` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_a` BIGINT NOT NULL COMMENT '用户A ID',
    `user_b` BIGINT DEFAULT NULL COMMENT '用户B ID',
    `love_date` DATE DEFAULT NULL COMMENT '恋爱开始日期',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1正常 0已解除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_a (`user_a`),
    INDEX idx_user_b (`user_b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情侣关系表';

-- 恋爱日记表
CREATE TABLE IF NOT EXISTS `diary` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL COMMENT '情侣关系ID',
    `creator_id` BIGINT NOT NULL COMMENT '创建者用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `location` VARCHAR(200) DEFAULT '' COMMENT '地点',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1正常 0已删除(回收站)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `delete_time` DATETIME DEFAULT NULL COMMENT '删除时间(回收站30天)',
    INDEX idx_couple_id (`couple_id`),
    INDEX idx_creator_id (`creator_id`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='恋爱日记表';

-- 日记媒体表
CREATE TABLE IF NOT EXISTS `diary_media` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `diary_id` BIGINT NOT NULL COMMENT '日记ID',
    `media_type` VARCHAR(20) NOT NULL COMMENT '媒体类型 image/video/audio',
    `file_url` VARCHAR(1000) NOT NULL COMMENT '文件URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_diary_id (`diary_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日记媒体表';

-- 共享待办表
CREATE TABLE IF NOT EXISTS `todo` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL COMMENT '情侣关系ID',
    `creator_id` BIGINT NOT NULL COMMENT '创建者用户ID',
    `executor_id` BIGINT DEFAULT NULL COMMENT '执行人用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '待办标题',
    `priority` VARCHAR(20) DEFAULT 'mid' COMMENT '优先级 high/mid/low',
    `deadline` DATETIME DEFAULT NULL COMMENT '截止时间',
    `repeat_type` VARCHAR(20) DEFAULT '' COMMENT '重复规则 daily/weekly/monthly',
    `status` TINYINT DEFAULT 0 COMMENT '状态 0待完成 1已完成 2已过期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_couple_id (`couple_id`),
    INDEX idx_executor_id (`executor_id`),
    INDEX idx_deadline (`deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='共享待办表';

-- 情侣任务表
CREATE TABLE IF NOT EXISTS `task` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL COMMENT '情侣关系ID',
    `title` VARCHAR(200) NOT NULL COMMENT '任务标题',
    `target_count` INT DEFAULT 1 COMMENT '目标次数',
    `current_count` INT DEFAULT 0 COMMENT '当前完成次数',
    `deadline` DATE DEFAULT NULL COMMENT '截止日期',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1进行中 2已完成 3已过期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_couple_id (`couple_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情侣任务表';

-- 任务打卡记录表
CREATE TABLE IF NOT EXISTS `task_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `user_id` BIGINT NOT NULL COMMENT '打卡用户ID',
    `confirm_user_id` BIGINT DEFAULT NULL COMMENT '确认用户ID',
    `status` TINYINT DEFAULT 0 COMMENT '状态 0待确认 1已确认',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_id (`task_id`),
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务打卡记录表';

-- 成就徽章表
CREATE TABLE IF NOT EXISTS `badge` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL COMMENT '情侣关系ID',
    `badge_type` VARCHAR(50) NOT NULL COMMENT '徽章类型 STREAK_7/TRAVEL/FITNESS',
    `title` VARCHAR(50) NOT NULL COMMENT '徽章名称',
    `icon` VARCHAR(100) DEFAULT '' COMMENT '徽章图标',
    `earned_date` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_couple_id (`couple_id`),
    UNIQUE KEY uk_couple_badge (`couple_id`, `badge_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成就徽章表';

-- 愿望清单表
CREATE TABLE IF NOT EXISTS `wish` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL COMMENT '情侣关系ID',
    `title` VARCHAR(200) NOT NULL COMMENT '愿望标题',
    `category` VARCHAR(50) DEFAULT '' COMMENT '分类 travel/life/growth',
    `target_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '目标金额',
    `current_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '当前金额',
    `target_date` DATE DEFAULT NULL COMMENT '目标日期',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1进行中 2已达成(梦想成真)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_couple_id (`couple_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='愿望清单表';

-- 纪念日表
CREATE TABLE IF NOT EXISTS `anniversary` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL COMMENT '情侣关系ID',
    `title` VARCHAR(200) NOT NULL COMMENT '纪念日标题',
    `anniversary_date` DATE NOT NULL COMMENT '纪念日日期',
    `remind_days` INT DEFAULT 0 COMMENT '提前提醒天数',
    `type` TINYINT DEFAULT 0 COMMENT '类型 0自定义 1系统(恋爱纪念日)',
    `icon` VARCHAR(20) DEFAULT '❤️' COMMENT '自定义图标',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1正常 0已删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_couple_id (`couple_id`),
    INDEX idx_anniversary_date (`anniversary_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='纪念日表';

-- 用户状态表
CREATE TABLE IF NOT EXISTS `user_status` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `status_name` VARCHAR(50) NOT NULL COMMENT '状态名称 工作中/学习中/睡觉中/运动中/游戏中/路上/自定义',
    `mood` VARCHAR(20) DEFAULT '' COMMENT '心情标签 开心/平静/难过/生气',
    `start_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    `duration_minutes` INT DEFAULT 0 COMMENT '持续时长(分钟)',
    `status` TINYINT DEFAULT 1 COMMENT '状态 1当前 0已结束',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (`user_id`),
    INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户状态表';
