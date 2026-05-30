-- =============================================================================
-- ISLAND（四六级岛）数据库 DDL
-- 引擎：MySQL 8.0+  |  字符集：utf8mb4  |  排序：utf8mb4_unicode_ci
-- 说明：MVP 单体架构；VIP 支付、评论等为第二期，表结构已预留
-- =============================================================================

CREATE DATABASE IF NOT EXISTS island
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE island;

-- -----------------------------------------------------------------------------
-- 1. 用户与鉴权
-- -----------------------------------------------------------------------------

CREATE TABLE `user` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `email`         VARCHAR(128)    NULL COMMENT '邮箱（MVP 登录）',
  `phone`         VARCHAR(20)     NULL COMMENT '手机号（第二期）',
  `password_hash` VARCHAR(255)    NULL COMMENT 'bcrypt 密码哈希',
  `nickname`      VARCHAR(64)     NOT NULL DEFAULT '岛民' COMMENT '昵称',
  `avatar_url`    VARCHAR(512)    NULL COMMENT '头像 URL',
  `vip_level`     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0普通 1VIP',
  `vip_expire_at` DATETIME        NULL COMMENT 'VIP 到期时间，NULL 表示非 VIP',
  `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `last_login_at` DATETIME        NULL COMMENT '最后登录时间',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_email` (`email`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  KEY `idx_user_vip` (`vip_level`, `vip_expire_at`)
) ENGINE=InnoDB COMMENT='用户主表';

CREATE TABLE `user_wechat` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '关联 user.id',
  `openid`     VARCHAR(64)     NOT NULL COMMENT '微信 OpenID',
  `unionid`    VARCHAR(64)     NULL COMMENT '微信 UnionID',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wechat_openid` (`openid`),
  UNIQUE KEY `uk_wechat_user` (`user_id`),
  CONSTRAINT `fk_wechat_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='微信绑定（第二期接入）';

-- -----------------------------------------------------------------------------
-- 2. VIP 订单（第二期支付，MVP 可手动改 user 表测 VIP）
-- -----------------------------------------------------------------------------

CREATE TABLE `vip_plan` (
  `id`            INT UNSIGNED    NOT NULL AUTO_INCREMENT,
  `code`          VARCHAR(32)     NOT NULL COMMENT '计划编码 monthly/yearly',
  `name`          VARCHAR(64)     NOT NULL COMMENT '展示名称',
  `price_cent`    INT UNSIGNED    NOT NULL COMMENT '价格（分）',
  `duration_days` INT UNSIGNED    NOT NULL COMMENT '有效天数',
  `is_active`     TINYINT         NOT NULL DEFAULT 1,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_code` (`code`)
) ENGINE=InnoDB COMMENT='VIP 套餐定义';

CREATE TABLE `vip_order` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_no`       VARCHAR(32)     NOT NULL COMMENT '业务订单号',
  `user_id`        BIGINT UNSIGNED NOT NULL,
  `plan_id`        INT UNSIGNED    NOT NULL,
  `amount_cent`    INT UNSIGNED    NOT NULL COMMENT '实付金额（分）',
  `pay_status`     TINYINT         NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已关闭 3已退款',
  `pay_channel`    VARCHAR(32)     NULL COMMENT 'wechat/alipay',
  `paid_at`        DATETIME        NULL,
  `vip_start_at`   DATETIME        NULL COMMENT 'VIP 生效开始',
  `vip_expire_at`  DATETIME        NULL COMMENT 'VIP 生效结束',
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_order_user` (`user_id`, `created_at` DESC),
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_order_plan` FOREIGN KEY (`plan_id`) REFERENCES `vip_plan` (`id`)
) ENGINE=InnoDB COMMENT='VIP 订单';

-- -----------------------------------------------------------------------------
-- 3. 文件资源（社区图片、头像；后期迁 OSS）
-- -----------------------------------------------------------------------------

CREATE TABLE `file_asset` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT UNSIGNED NULL COMMENT '上传者，系统资源可为 NULL',
  `storage_type`  ENUM('local','oss') NOT NULL DEFAULT 'local',
  `bucket`        VARCHAR(64)     NULL COMMENT 'OSS bucket，local 时为空',
  `object_key`    VARCHAR(512)    NOT NULL COMMENT '相对路径或 OSS key',
  `original_name` VARCHAR(255)    NULL COMMENT '原始文件名',
  `mime_type`     VARCHAR(64)     NOT NULL COMMENT 'image/jpeg 等',
  `size_bytes`    INT UNSIGNED    NOT NULL DEFAULT 0,
  `width`         INT UNSIGNED    NULL,
  `height`        INT UNSIGNED    NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_file_user` (`user_id`),
  CONSTRAINT `fk_file_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='上传文件元数据';

-- -----------------------------------------------------------------------------
-- 4. 内容来源与合规
-- -----------------------------------------------------------------------------

CREATE TABLE `content_source` (
  `id`           INT UNSIGNED    NOT NULL AUTO_INCREMENT,
  `name`         VARCHAR(128)    NOT NULL COMMENT '来源名称',
  `source_type`  ENUM('original','mock_exam','ted','bilibili','book','other') NOT NULL,
  `source_url`   VARCHAR(512)    NULL COMMENT '原始链接',
  `license_note` VARCHAR(512)    NULL COMMENT '授权/合规说明',
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='内容来源与合规记录';

-- -----------------------------------------------------------------------------
-- 5. 阅读模块
-- -----------------------------------------------------------------------------

CREATE TABLE `reading_chapter` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title`        VARCHAR(200)    NOT NULL COMMENT '章节标题',
  `slug`         VARCHAR(100)    NOT NULL COMMENT 'URL 友好标识',
  `summary`      VARCHAR(500)    NULL COMMENT '摘要',
  `content_html` MEDIUMTEXT      NOT NULL COMMENT '章节正文 HTML',
  `sort_order`   INT             NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `is_vip`       TINYINT         NOT NULL DEFAULT 0 COMMENT '1=高级技巧，仅 VIP',
  `status`       TINYINT         NOT NULL DEFAULT 1 COMMENT '1发布 0草稿',
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reading_slug` (`slug`),
  KEY `idx_reading_sort` (`status`, `sort_order`)
) ENGINE=InnoDB COMMENT='阅读技巧章节';

CREATE TABLE `reading_passage` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `chapter_id`   BIGINT UNSIGNED NULL COMMENT '所属章节，可为空表示独立练习',
  `title`        VARCHAR(200)    NOT NULL COMMENT '篇章标题',
  `content_en`   MEDIUMTEXT      NOT NULL COMMENT '英文原文',
  `word_count`   INT UNSIGNED    NULL COMMENT '词数',
  `difficulty`   ENUM('cet4','cet6') NOT NULL DEFAULT 'cet4',
  `is_mock`      TINYINT         NOT NULL DEFAULT 1 COMMENT '1=自编模拟题',
  `source_id`    INT UNSIGNED    NULL COMMENT 'content_source.id',
  `sort_order`   INT             NOT NULL DEFAULT 0,
  `status`       TINYINT         NOT NULL DEFAULT 1,
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_passage_chapter` (`chapter_id`),
  KEY `idx_passage_difficulty` (`difficulty`, `status`),
  CONSTRAINT `fk_passage_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `reading_chapter` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_passage_source` FOREIGN KEY (`source_id`) REFERENCES `content_source` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='阅读练习篇章';

CREATE TABLE `reading_question` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `passage_id`    BIGINT UNSIGNED NOT NULL,
  `question_type` ENUM('single','multiple','true_false') NOT NULL DEFAULT 'single',
  `stem`          TEXT            NOT NULL COMMENT '题干',
  `explanation`   TEXT            NULL COMMENT '答案解析',
  `sort_order`    INT             NOT NULL DEFAULT 0,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rq_passage` (`passage_id`, `sort_order`),
  CONSTRAINT `fk_rq_passage` FOREIGN KEY (`passage_id`) REFERENCES `reading_passage` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='阅读选择题';

CREATE TABLE `reading_question_option` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `question_id` BIGINT UNSIGNED NOT NULL,
  `label`       CHAR(1)         NOT NULL COMMENT 'A/B/C/D',
  `content`     VARCHAR(512)    NOT NULL COMMENT '选项内容',
  `is_correct`  TINYINT         NOT NULL DEFAULT 0 COMMENT '1=正确答案',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_option_label` (`question_id`, `label`),
  CONSTRAINT `fk_rqo_question` FOREIGN KEY (`question_id`) REFERENCES `reading_question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='阅读题选项';

CREATE TABLE `user_reading_progress` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `chapter_id`  BIGINT UNSIGNED NOT NULL,
  `is_finished` TINYINT         NOT NULL DEFAULT 0,
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_urp_user_chapter` (`user_id`, `chapter_id`),
  CONSTRAINT `fk_urp_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_urp_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `reading_chapter` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='用户阅读章节进度';

-- -----------------------------------------------------------------------------
-- 6. 翻译模块
-- -----------------------------------------------------------------------------

CREATE TABLE `translation_chapter` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title`        VARCHAR(200)    NOT NULL,
  `slug`         VARCHAR(100)    NOT NULL,
  `summary`      VARCHAR(500)    NULL,
  `content_html` MEDIUMTEXT      NOT NULL,
  `sort_order`   INT             NOT NULL DEFAULT 0,
  `is_vip`       TINYINT         NOT NULL DEFAULT 0,
  `status`       TINYINT         NOT NULL DEFAULT 1,
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_translation_slug` (`slug`),
  KEY `idx_translation_sort` (`status`, `sort_order`)
) ENGINE=InnoDB COMMENT='翻译技巧章节';

CREATE TABLE `translation_question` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `chapter_id`        BIGINT UNSIGNED NULL,
  `direction`         ENUM('zh2en','en2zh') NOT NULL DEFAULT 'zh2en' COMMENT '翻译方向',
  `prompt_zh`         TEXT            NULL COMMENT '中文题干（中译英时用）',
  `prompt_en`         TEXT            NULL COMMENT '英文题干（英译中时用）',
  `reference_answer`  TEXT            NOT NULL COMMENT '参考答案',
  `difficulty`        ENUM('cet4','cet6') NOT NULL DEFAULT 'cet4',
  `is_mock`           TINYINT         NOT NULL DEFAULT 1,
  `is_vip`            TINYINT         NOT NULL DEFAULT 0 COMMENT '1=仅 VIP 可练',
  `source_id`         INT UNSIGNED    NULL,
  `sort_order`        INT             NOT NULL DEFAULT 0,
  `status`            TINYINT         NOT NULL DEFAULT 1,
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tq_chapter` (`chapter_id`),
  CONSTRAINT `fk_tq_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `translation_chapter` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_tq_source` FOREIGN KEY (`source_id`) REFERENCES `content_source` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='翻译练习题';

CREATE TABLE `translation_submission` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT UNSIGNED NOT NULL,
  `question_id`     BIGINT UNSIGNED NOT NULL,
  `user_answer`     TEXT            NOT NULL COMMENT '用户译文',
  `score`           TINYINT UNSIGNED NULL COMMENT 'AI 评分 0-100',
  `overall_comment` VARCHAR(1000)   NULL COMMENT '总评',
  `errors_json`     JSON            NULL COMMENT '错误点 [{span,suggestion,reason}]',
  `reference_hint`  TEXT            NULL COMMENT '参考表达提示',
  `ai_model`        VARCHAR(64)     NULL COMMENT '使用的模型名',
  `ai_raw_response` JSON            NULL COMMENT 'LLM 原始响应（调试/审计）',
  `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ts_user` (`user_id`, `created_at` DESC),
  KEY `idx_ts_question` (`question_id`),
  CONSTRAINT `fk_ts_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ts_question` FOREIGN KEY (`question_id`) REFERENCES `translation_question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='翻译提交与 AI 批改记录';

-- -----------------------------------------------------------------------------
-- 7. 词汇模块
-- -----------------------------------------------------------------------------

CREATE TABLE `vocabulary` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `word`         VARCHAR(64)     NOT NULL COMMENT '单词/短语',
  `phonetic`     VARCHAR(64)     NULL COMMENT '音标',
  `meaning_zh`   VARCHAR(512)    NOT NULL COMMENT '中文释义',
  `example_en`   VARCHAR(512)    NULL,
  `example_zh`   VARCHAR(512)    NULL,
  `collocation`  VARCHAR(256)    NULL COMMENT '常见搭配',
  `difficulty`   ENUM('cet4','cet6') NOT NULL DEFAULT 'cet4',
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vocab_word` (`word`),
  KEY `idx_vocab_difficulty` (`difficulty`)
) ENGINE=InnoDB COMMENT='词汇库';

CREATE TABLE `sentence_pattern` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `pattern_en`  VARCHAR(512)    NOT NULL COMMENT '句式英文',
  `pattern_zh`  VARCHAR(512)    NOT NULL COMMENT '句式中文',
  `example_en`  VARCHAR(512)    NULL,
  `example_zh`  VARCHAR(512)    NULL,
  `category`    VARCHAR(64)     NULL COMMENT '分类：写作/口语/翻译',
  `difficulty`  ENUM('cet4','cet6') NOT NULL DEFAULT 'cet4',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pattern_category` (`category`)
) ENGINE=InnoDB COMMENT='搭配句式库';

CREATE TABLE `user_vocabulary` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `vocabulary_id` BIGINT UNSIGNED NOT NULL,
  `source_type` ENUM('manual','video','reading','translation') NOT NULL DEFAULT 'manual',
  `source_id`   BIGINT UNSIGNED NULL COMMENT '来源业务 ID',
  `note`        VARCHAR(512)    NULL COMMENT '用户笔记',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uv_user_vocab` (`user_id`, `vocabulary_id`),
  CONSTRAINT `fk_uv_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_uv_vocab` FOREIGN KEY (`vocabulary_id`) REFERENCES `vocabulary` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='用户生词本';

-- -----------------------------------------------------------------------------
-- 8. 双语视频模块
-- -----------------------------------------------------------------------------

CREATE TABLE `video_series` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title`       VARCHAR(200)    NOT NULL COMMENT '系列名，如 TEco Lab',
  `description` VARCHAR(500)    NULL,
  `cover_url`   VARCHAR(512)    NULL,
  `sort_order`  INT             NOT NULL DEFAULT 0,
  `status`      TINYINT         NOT NULL DEFAULT 1,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='视频系列/专栏';

CREATE TABLE `video` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `series_id`     BIGINT UNSIGNED NULL COMMENT '所属系列',
  `title`         VARCHAR(200)    NOT NULL,
  `description`   VARCHAR(1000)   NULL,
  `cover_url`     VARCHAR(512)    NULL,
  `storage_type`  ENUM('embed','oss') NOT NULL DEFAULT 'embed' COMMENT 'MVP=embed，后期 4K=oss',
  `provider`      ENUM('bilibili','youtube','self') NOT NULL DEFAULT 'bilibili',
  `source_url`    VARCHAR(512)    NOT NULL COMMENT 'B 站原页 URL',
  `embed_bvid`    VARCHAR(20)     NULL COMMENT 'BV 号，如 BV1qP4y1M7cb',
  `embed_aid`     BIGINT          NULL COMMENT 'B 站 av/aid（可选）',
  `embed_cid`     BIGINT          NULL COMMENT 'B 站分 P cid（可选）',
  `play_url`      VARCHAR(512)    NULL COMMENT 'storage_type=oss 时的 CDN 地址',
  `duration_sec`  INT UNSIGNED    NULL COMMENT '时长（秒）',
  `difficulty`    ENUM('easy','medium','hard') NOT NULL DEFAULT 'medium',
  `is_vip`        TINYINT         NOT NULL DEFAULT 0 COMMENT '1=专属视频',
  `view_count`    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '站内播放计数',
  `sort_order`    INT             NOT NULL DEFAULT 0,
  `source_id`     INT UNSIGNED    NULL,
  `license_note`  VARCHAR(512)    NULL,
  `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_video_series` (`series_id`, `sort_order`),
  KEY `idx_video_vip` (`is_vip`, `status`),
  KEY `idx_video_bvid` (`embed_bvid`),
  CONSTRAINT `fk_video_series` FOREIGN KEY (`series_id`) REFERENCES `video_series` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_video_source` FOREIGN KEY (`source_id`) REFERENCES `content_source` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='双语视频主表';

CREATE TABLE `video_sentence` (
  `id`        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `video_id`  BIGINT UNSIGNED NOT NULL,
  `seq`       INT UNSIGNED    NOT NULL COMMENT '句子序号，从 1 开始',
  `start_ms`  INT UNSIGNED    NOT NULL COMMENT '开始毫秒',
  `end_ms`    INT UNSIGNED    NOT NULL COMMENT '结束毫秒',
  `text_en`   TEXT            NOT NULL,
  `text_zh`   TEXT            NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vs_video_seq` (`video_id`, `seq`),
  KEY `idx_vs_video_time` (`video_id`, `start_ms`),
  CONSTRAINT `fk_vs_video` FOREIGN KEY (`video_id`) REFERENCES `video` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='视频句级字幕时间轴';

CREATE TABLE `video_favorite` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `video_id`   BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vf_user_video` (`user_id`, `video_id`),
  CONSTRAINT `fk_vf_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_vf_video` FOREIGN KEY (`video_id`) REFERENCES `video` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='用户收藏视频';

CREATE TABLE `user_video_progress` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`          BIGINT UNSIGNED NOT NULL,
  `video_id`         BIGINT UNSIGNED NOT NULL,
  `last_sentence_seq` INT UNSIGNED   NULL COMMENT '最后学习到的句子序号',
  `last_position_ms`  INT UNSIGNED   NULL COMMENT '最后播放位置',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uvp_user_video` (`user_id`, `video_id`),
  CONSTRAINT `fk_uvp_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_uvp_video` FOREIGN KEY (`video_id`) REFERENCES `video` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='用户视频学习进度';

-- -----------------------------------------------------------------------------
-- 9. 社区 Feed
-- -----------------------------------------------------------------------------

CREATE TABLE `post` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `content`    TEXT            NOT NULL COMMENT '文字内容',
  `like_count` INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '冗余计数',
  `status`     TINYINT         NOT NULL DEFAULT 1 COMMENT '1正常 0隐藏 2审核中',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post_feed` (`status`, `created_at` DESC),
  KEY `idx_post_user` (`user_id`, `created_at` DESC),
  CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='社区帖子（仅文字）';

CREATE TABLE `post_image` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `post_id`    BIGINT UNSIGNED NOT NULL,
  `file_id`    BIGINT UNSIGNED NOT NULL COMMENT 'file_asset.id',
  `sort_order` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_pi_post` (`post_id`, `sort_order`),
  CONSTRAINT `fk_pi_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pi_file` FOREIGN KEY (`file_id`) REFERENCES `file_asset` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='帖子图片（多图）';

CREATE TABLE `post_like` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `post_id`    BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pl_user_post` (`user_id`, `post_id`),
  CONSTRAINT `fk_pl_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pl_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='帖子点赞';

CREATE TABLE `post_comment` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `post_id`    BIGINT UNSIGNED NOT NULL,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `content`    VARCHAR(1000)   NOT NULL,
  `status`     TINYINT         NOT NULL DEFAULT 1,
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pc_post` (`post_id`, `created_at`),
  CONSTRAINT `fk_pc_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pc_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='帖子评论（第二期，MVP 可不实现 API）';

-- -----------------------------------------------------------------------------
-- 10. 系统
-- -----------------------------------------------------------------------------

CREATE TABLE `admin_audit_log` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `admin_id`    BIGINT UNSIGNED NULL COMMENT '操作人 user.id',
  `action`      VARCHAR(64)     NOT NULL COMMENT '操作类型',
  `target_type` VARCHAR(64)     NULL COMMENT '目标实体',
  `target_id`   BIGINT UNSIGNED NULL,
  `detail_json` JSON            NULL,
  `ip`          VARCHAR(45)     NULL,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_time` (`created_at` DESC)
) ENGINE=InnoDB COMMENT='后台操作审计';

-- -----------------------------------------------------------------------------
-- 11. 初始种子（可选，开发环境）
-- -----------------------------------------------------------------------------

INSERT INTO `content_source` (`name`, `source_type`, `source_url`, `license_note`) VALUES
('ISLAND 自编模拟题', 'original', NULL, '平台原创，非真题'),
('TED 官方', 'ted', 'https://www.ted.com', 'Embed 播放，非商用；VIP 商用需另行授权');

INSERT INTO `vip_plan` (`code`, `name`, `price_cent`, `duration_days`) VALUES
('monthly', '月度 VIP', 1990, 30),
('yearly',  '年度 VIP', 9900, 365);
