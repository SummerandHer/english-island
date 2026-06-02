-- 词汇模块扩展 + 每日复习队列

ALTER TABLE `vocabulary`
  ADD COLUMN `part_of_speech` VARCHAR(16) NULL COMMENT '词性' AFTER `phonetic`,
  ADD COLUMN `freq_rank` INT UNSIGNED NULL COMMENT '考频排序，越小越高频' AFTER `difficulty`,
  ADD COLUMN `phrases_json` JSON NULL COMMENT '短语 [{en,zh}]' AFTER `collocation`,
  ADD COLUMN `source_note` VARCHAR(128) NULL COMMENT '词库来源说明' AFTER `phrases_json`,
  ADD KEY `idx_vocab_freq` (`difficulty`, `freq_rank`);

CREATE TABLE `user_vocab_review` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT UNSIGNED NOT NULL,
  `vocabulary_id`   BIGINT UNSIGNED NOT NULL,
  `familiarity`     TINYINT         NOT NULL DEFAULT 0 COMMENT '0-5',
  `next_review_at`  DATETIME        NOT NULL,
  `last_review_at`  DATETIME        NULL,
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uvr_user_vocab` (`user_id`, `vocabulary_id`),
  KEY `idx_uvr_user_next` (`user_id`, `next_review_at`),
  CONSTRAINT `fk_uvr_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_uvr_vocab` FOREIGN KEY (`vocabulary_id`) REFERENCES `vocabulary` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='用户词汇复习计划';
