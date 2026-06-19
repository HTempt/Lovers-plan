-- 纪念日表添加 icon 字段
ALTER TABLE `anniversary`
    ADD COLUMN `icon` VARCHAR(20) DEFAULT '❤️' COMMENT '自定义图标' AFTER `type`;
