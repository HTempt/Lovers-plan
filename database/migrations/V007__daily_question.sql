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
