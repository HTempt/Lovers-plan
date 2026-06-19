-- 用户表添加手机号字段
ALTER TABLE `user`
    ADD COLUMN `phone` VARCHAR(20) DEFAULT '' COMMENT '手机号' AFTER `gender`;
