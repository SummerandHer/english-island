-- 视频词汇量冗余字段，列表页快速展示
ALTER TABLE `video`
  ADD COLUMN `vocab_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '去重英文词汇数' AFTER `duration_sec`;
