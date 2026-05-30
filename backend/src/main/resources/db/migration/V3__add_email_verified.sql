ALTER TABLE `user`
    ADD COLUMN `email_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证' AFTER `email`;

UPDATE `user` SET `email_verified` = 1 WHERE `email` IS NOT NULL;
