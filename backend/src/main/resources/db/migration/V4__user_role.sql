ALTER TABLE `user`
  ADD COLUMN `role` VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT 'USER|ADMIN' AFTER `status`;

-- 将首个注册用户设为管理员（可按需改邮箱后手动 UPDATE）
UPDATE `user` SET `role` = 'ADMIN' ORDER BY `id` LIMIT 1;
