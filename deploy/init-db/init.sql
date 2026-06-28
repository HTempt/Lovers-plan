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








-- 纪念日表添加 icon 字段
ALTER TABLE `anniversary`
    ADD COLUMN `icon` VARCHAR(20) DEFAULT '❤️' COMMENT '自定义图标' AFTER `type`;
-- 用户表添加手机号字段
ALTER TABLE `user`
    ADD COLUMN `phone` VARCHAR(20) DEFAULT '' COMMENT '手机号' AFTER `gender`;
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
-- 双人问答
-- V007: daily_question + daily_answer + seed questions

-- 问题池
CREATE TABLE IF NOT EXISTS `question` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `question_text` VARCHAR(500) NOT NULL COMMENT '问题内容',
    `option_a` VARCHAR(200) DEFAULT NULL COMMENT '选项A',
    `option_b` VARCHAR(200) DEFAULT NULL COMMENT '选项B',
    `option_c` VARCHAR(200) DEFAULT NULL COMMENT '选项C',
    `option_d` VARCHAR(200) DEFAULT NULL COMMENT '选项D',
    `question_type` VARCHAR(30) NOT NULL COMMENT '问题类型 memory/preference/emotion/future/fun',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type (`question_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题池';

-- 每日答题记录（每天每对情侣一条）
CREATE TABLE IF NOT EXISTS `daily_question` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL COMMENT '情侣关系ID',
    `question_id` BIGINT NOT NULL COMMENT '问题ID',
    `question_date` DATE NOT NULL COMMENT '答题日期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_couple_date (`couple_id`, `question_date`),
    INDEX idx_couple_id (`couple_id`),
    INDEX idx_question_date (`question_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日答题记录';

-- 答题答案表
CREATE TABLE IF NOT EXISTS `daily_answer` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `daily_question_id` BIGINT NOT NULL COMMENT '每日答题记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `answer` VARCHAR(10) NOT NULL COMMENT '答案选项 A/B/C/D',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_record_user (`daily_question_id`, `user_id`),
    INDEX idx_daily_question_id (`daily_question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题答案表';

-- ====== 种子问题数据 ======
INSERT INTO `question` (`question_text`, `option_a`, `option_b`, `option_c`, `option_d`, `question_type`) VALUES
-- 回忆类
('我们第一次见面是在哪里？', '咖啡馆', '图书馆', '朋友聚会', '学校', 'memory'),
('我们第一次约会做了什么？', '看电影', '吃饭', '散步', '逛街', 'memory'),
('我们第一次牵手是谁主动的？', '我', 'TA', '自然而然', '不记得了', 'memory'),
('我们第一次吵架的原因是什么？', '误会', '吃醋', '小事', '不记得了', 'memory'),
('我们第一次旅行去了哪里？', '海边', '山里', '城市', '国外', 'memory'),

-- 偏好类
('TA最喜欢吃什么？', '火锅', '烤肉', '日料', '川菜', 'preference'),
('TA最喜欢的电影类型？', '爱情片', '科幻片', '喜剧片', '悬疑片', 'preference'),
('TA周末最想做什么？', '宅家', '出去玩', '运动', '看电影', 'preference'),
('TA最怕什么？', '虫子', '黑', '高', '孤独', 'preference'),
('TA最喜欢的季节？', '春天', '夏天', '秋天', '冬天', 'preference'),

-- 情感类
('如果只能选一个，你会选？', '被爱', '去爱', '自由', '陪伴', 'emotion'),
('吵架后谁先道歉？', '我', 'TA', '互相', '冷战', 'emotion'),
('最让TA感动的瞬间？', '惊喜礼物', '陪伴', '记得细节', '道歉', 'emotion'),
('TA的口头禅是什么？', '随便', '你定', '哈哈哈', '好的', 'emotion'),
('在TA心里你最像什么？', '太阳', '月亮', '星星', '风', 'emotion'),

-- 未来类
('最想和TA一起去的地方？', '日本', '欧洲', '海边', '雪山', 'future'),
('希望几年后结婚？', '1年', '2年', '3年', '随缘', 'future'),
('以后想养什么宠物？', '猫', '狗', '兔子', '不养', 'future'),
('最想和TA一起学的技能？', '做饭', '摄影', '跳舞', '乐器', 'future'),
('退休后想过什么样的生活？', '乡下', '海边', '城市', '旅行', 'future'),

-- 娱乐类
('如果变成动物，对方是什么？', '猫', '狗', '兔子', '熊猫', 'fun'),
('一起出门谁负责选餐厅？', '我', 'TA', '一起商量', '随机', 'fun'),
('对方睡觉会打呼吗？', '会', '不会', '偶尔', '不知道', 'fun'),
('最想收到的礼物？', '手写信', '包包', '零食', '旅行', 'fun'),
('下辈子还要在一起吗？', '要', '不要', '看情况', '下辈子再说', 'fun');
-- 情侣成就系统
-- V008: achievement definitions + user achievement tracking

-- 成就定义表
CREATE TABLE IF NOT EXISTS `achievement` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `category` VARCHAR(20) NOT NULL COMMENT '分类: anniversary/diary/footprint/task/capsule/quiz/growth',
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '唯一标识码',
    `name` VARCHAR(100) NOT NULL COMMENT '成就名称',
    `description` VARCHAR(500) DEFAULT '' COMMENT '成就描述',
    `icon` VARCHAR(10) DEFAULT '🏆' COMMENT '图标',
    `rarity` TINYINT DEFAULT 1 COMMENT '稀有度: 1=普通 2=稀有',
    `growth_reward` INT DEFAULT 20 COMMENT '解锁奖励成长值',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `hidden` TINYINT DEFAULT 0 COMMENT '是否隐藏(解锁前不可见)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (`category`),
    INDEX idx_code (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成就定义表';

-- 用户成就解锁表
CREATE TABLE IF NOT EXISTS `user_achievement` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `couple_id` BIGINT NOT NULL COMMENT '情侣关系ID',
    `achievement_id` BIGINT NOT NULL COMMENT '成就ID',
    `unlocked_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '解锁时间',
    UNIQUE KEY `uk_couple_achievement` (`couple_id`, `achievement_id`),
    INDEX idx_couple_id (`couple_id`),
    INDEX idx_achievement_id (`achievement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户成就解锁表';

-- ===== 插入30个成就定义 =====

-- ❤️ 纪念类 (5)
INSERT INTO `achievement` (`category`, `code`, `name`, `description`, `icon`, `rarity`, `growth_reward`, `sort_order`, `hidden`) VALUES
('anniversary', 'bind_couple', '心心相印', '成功绑定情侣关系', '💕', 1, 20, 1, 0),
('anniversary', 'days_100', '百日誓约', '在一起100天', '❤️', 1, 20, 2, 0),
('anniversary', 'days_200', '情比金坚', '在一起200天', '❤️', 1, 20, 3, 0),
('anniversary', 'days_300', '三生有幸', '在一起300天', '❤️', 1, 20, 4, 0),
('anniversary', 'days_365', '一周年快乐', '在一起一周年', '💖', 2, 50, 5, 0);

-- 📷 日记类 (5)
INSERT INTO `achievement` (`category`, `code`, `name`, `description`, `icon`, `rarity`, `growth_reward`, `sort_order`, `hidden`) VALUES
('diary', 'diary_1', '初露锋芒', '写下第一篇日记', '📝', 1, 20, 6, 0),
('diary', 'diary_10', '点滴记录', '累计10篇日记', '📓', 1, 20, 7, 0),
('diary', 'diary_50', '时光笔耕', '累计50篇日记', '📔', 1, 20, 8, 0),
('diary', 'diary_100', '百篇记忆', '累计100篇日记', '📖', 2, 50, 9, 0),
('diary', 'diary_photo_10', '光影拾贝', '上传10张带图日记', '📷', 1, 20, 10, 0);

-- 🌏 足迹类 (5)
INSERT INTO `achievement` (`category`, `code`, `name`, `description`, `icon`, `rarity`, `growth_reward`, `sort_order`, `hidden`) VALUES
('footprint', 'city_1', '第一步', '到达第1座城市', '📍', 1, 20, 11, 0),
('footprint', 'city_3', '周末出游', '一起到达3座城市', '🗺️', 1, 20, 12, 0),
('footprint', 'city_5', '旅行达人', '一起到达5座城市', '🌍', 1, 20, 13, 0),
('footprint', 'city_10', '行者无疆', '一起到达10座城市', '🌏', 2, 50, 14, 0),
('footprint', 'province_cross', '跨省之旅', '足迹覆盖2个以上省份', '🚄', 1, 20, 15, 1);

-- 🎯 任务类 (5)
INSERT INTO `achievement` (`category`, `code`, `name`, `description`, `icon`, `rarity`, `growth_reward`, `sort_order`, `hidden`) VALUES
('task', 'task_1', '初次挑战', '完成第1个情侣任务', '🎯', 1, 20, 16, 0),
('task', 'task_10', '坚持不懈', '累计完成10个任务', '🎯', 1, 20, 17, 0),
('task', 'task_50', '行动派', '累计完成50个任务', '🏅', 1, 20, 18, 0),
('task', 'task_100', '百战成神', '累计完成100个任务', '🏆', 2, 50, 19, 0),
('task', 'checkin_7', '七日之约', '连续打卡7天', '🔥', 1, 20, 20, 0);

-- 💌 胶囊类 (3)
INSERT INTO `achievement` (`category`, `code`, `name`, `description`, `icon`, `rarity`, `growth_reward`, `sort_order`, `hidden`) VALUES
('capsule', 'capsule_1', '未来来信', '封存第1个时光胶囊', '💌', 1, 20, 21, 0),
('capsule', 'capsule_open_1', '时空信使', '开启第1个时光胶囊', '📬', 1, 20, 22, 0),
('capsule', 'capsule_dual', '双人合璧', '双方共同完成一个胶囊', '💞', 2, 50, 23, 1);

-- 💬 问答类 (3)
INSERT INTO `achievement` (`category`, `code`, `name`, `description`, `icon`, `rarity`, `growth_reward`, `sort_order`, `hidden`) VALUES
('quiz', 'quiz_1', '初次问答', '完成第1次双人问答', '❓', 1, 20, 24, 0),
('quiz', 'quiz_perfect', '心有灵犀', '默契度达到100%', '💯', 1, 20, 25, 0),
('quiz', 'quiz_7', '默契养成', '连续7天完成问答', '🧠', 2, 50, 26, 0);

-- 🌱 成长类 (4)
INSERT INTO `achievement` (`category`, `code`, `name`, `description`, `icon`, `rarity`, `growth_reward`, `sort_order`, `hidden`) VALUES
('growth', 'tree_lv2', '破土而出', '爱情树达到Lv2', '🌱', 1, 20, 27, 0),
('growth', 'tree_lv5', '茁壮成长', '爱情树达到Lv5', '🌿', 1, 20, 28, 0),
('growth', 'tree_lv10', '枝繁叶茂', '爱情树达到Lv10', '🌳', 2, 50, 29, 0),
('growth', 'sign_30', '爱的习惯', '累计签到30天', '📅', 1, 20, 30, 0);
