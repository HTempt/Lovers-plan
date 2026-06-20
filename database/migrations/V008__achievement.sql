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
