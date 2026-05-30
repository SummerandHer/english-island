USE island;

-- -----------------------------------------------------------------------------
-- 1. 鐢ㄦ埛涓庨壌鏉?
-- -----------------------------------------------------------------------------

CREATE TABLE `user` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '鐢ㄦ埛ID',
  `email`         VARCHAR(128)    NULL COMMENT '閭锛圡VP 鐧诲綍锛?,
  `phone`         VARCHAR(20)     NULL COMMENT '鎵嬫満鍙凤紙绗簩鏈燂級',
  `password_hash` VARCHAR(255)    NULL COMMENT 'bcrypt 瀵嗙爜鍝堝笇',
  `nickname`      VARCHAR(64)     NOT NULL DEFAULT '宀涙皯' COMMENT '鏄电О',
  `avatar_url`    VARCHAR(512)    NULL COMMENT '澶村儚 URL',
  `vip_level`     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0鏅€?1VIP',
  `vip_expire_at` DATETIME        NULL COMMENT 'VIP 鍒版湡鏃堕棿锛孨ULL 琛ㄧず闈?VIP',
  `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1姝ｅ父 0绂佺敤',
  `last_login_at` DATETIME        NULL COMMENT '鏈€鍚庣櫥褰曟椂闂?,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_email` (`email`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  KEY `idx_user_vip` (`vip_level`, `vip_expire_at`)
) ENGINE=InnoDB COMMENT='鐢ㄦ埛涓昏〃';

CREATE TABLE `user_wechat` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '鍏宠仈 user.id',
  `openid`     VARCHAR(64)     NOT NULL COMMENT '寰俊 OpenID',
  `unionid`    VARCHAR(64)     NULL COMMENT '寰俊 UnionID',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wechat_openid` (`openid`),
  UNIQUE KEY `uk_wechat_user` (`user_id`),
  CONSTRAINT `fk_wechat_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='寰俊缁戝畾锛堢浜屾湡鎺ュ叆锛?;

-- -----------------------------------------------------------------------------
-- 2. VIP 璁㈠崟锛堢浜屾湡鏀粯锛孧VP 鍙墜鍔ㄦ敼 user 琛ㄦ祴 VIP锛?
-- -----------------------------------------------------------------------------

CREATE TABLE `vip_plan` (
  `id`            INT UNSIGNED    NOT NULL AUTO_INCREMENT,
  `code`          VARCHAR(32)     NOT NULL COMMENT '璁″垝缂栫爜 monthly/yearly',
  `name`          VARCHAR(64)     NOT NULL COMMENT '灞曠ず鍚嶇О',
  `price_cent`    INT UNSIGNED    NOT NULL COMMENT '浠锋牸锛堝垎锛?,
  `duration_days` INT UNSIGNED    NOT NULL COMMENT '鏈夋晥澶╂暟',
  `is_active`     TINYINT         NOT NULL DEFAULT 1,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_code` (`code`)
) ENGINE=InnoDB COMMENT='VIP 濂楅瀹氫箟';

CREATE TABLE `vip_order` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_no`       VARCHAR(32)     NOT NULL COMMENT '涓氬姟璁㈠崟鍙?,
  `user_id`        BIGINT UNSIGNED NOT NULL,
  `plan_id`        INT UNSIGNED    NOT NULL,
  `amount_cent`    INT UNSIGNED    NOT NULL COMMENT '瀹炰粯閲戦锛堝垎锛?,
  `pay_status`     TINYINT         NOT NULL DEFAULT 0 COMMENT '0寰呮敮浠?1宸叉敮浠?2宸插叧闂?3宸查€€娆?,
  `pay_channel`    VARCHAR(32)     NULL COMMENT 'wechat/alipay',
  `paid_at`        DATETIME        NULL,
  `vip_start_at`   DATETIME        NULL COMMENT 'VIP 鐢熸晥寮€濮?,
  `vip_expire_at`  DATETIME        NULL COMMENT 'VIP 鐢熸晥缁撴潫',
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_order_user` (`user_id`, `created_at` DESC),
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_order_plan` FOREIGN KEY (`plan_id`) REFERENCES `vip_plan` (`id`)
) ENGINE=InnoDB COMMENT='VIP 璁㈠崟';

-- -----------------------------------------------------------------------------
-- 3. 鏂囦欢璧勬簮锛堢ぞ鍖哄浘鐗囥€佸ご鍍忥紱鍚庢湡杩?OSS锛?
-- -----------------------------------------------------------------------------

CREATE TABLE `file_asset` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT UNSIGNED NULL COMMENT '涓婁紶鑰咃紝绯荤粺璧勬簮鍙负 NULL',
  `storage_type`  ENUM('local','oss') NOT NULL DEFAULT 'local',
  `bucket`        VARCHAR(64)     NULL COMMENT 'OSS bucket锛宭ocal 鏃朵负绌?,
  `object_key`    VARCHAR(512)    NOT NULL COMMENT '鐩稿璺緞鎴?OSS key',
  `original_name` VARCHAR(255)    NULL COMMENT '鍘熷鏂囦欢鍚?,
  `mime_type`     VARCHAR(64)     NOT NULL COMMENT 'image/jpeg 绛?,
  `size_bytes`    INT UNSIGNED    NOT NULL DEFAULT 0,
  `width`         INT UNSIGNED    NULL,
  `height`        INT UNSIGNED    NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_file_user` (`user_id`),
  CONSTRAINT `fk_file_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='涓婁紶鏂囦欢鍏冩暟鎹?;

-- -----------------------------------------------------------------------------
-- 4. 鍐呭鏉ユ簮涓庡悎瑙?
-- -----------------------------------------------------------------------------

CREATE TABLE `content_source` (
  `id`           INT UNSIGNED    NOT NULL AUTO_INCREMENT,
  `name`         VARCHAR(128)    NOT NULL COMMENT '鏉ユ簮鍚嶇О',
  `source_type`  ENUM('original','mock_exam','ted','bilibili','book','other') NOT NULL,
  `source_url`   VARCHAR(512)    NULL COMMENT '鍘熷閾炬帴',
  `license_note` VARCHAR(512)    NULL COMMENT '鎺堟潈/鍚堣璇存槑',
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='鍐呭鏉ユ簮涓庡悎瑙勮褰?;

-- -----------------------------------------------------------------------------
-- 5. 闃呰妯″潡
-- -----------------------------------------------------------------------------

CREATE TABLE `reading_chapter` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title`        VARCHAR(200)    NOT NULL COMMENT '绔犺妭鏍囬',
  `slug`         VARCHAR(100)    NOT NULL COMMENT 'URL 鍙嬪ソ鏍囪瘑',
  `summary`      VARCHAR(500)    NULL COMMENT '鎽樿',
  `content_html` MEDIUMTEXT      NOT NULL COMMENT '绔犺妭姝ｆ枃 HTML',
  `sort_order`   INT             NOT NULL DEFAULT 0 COMMENT '鎺掑簭锛岃秺灏忚秺闈犲墠',
  `is_vip`       TINYINT         NOT NULL DEFAULT 0 COMMENT '1=楂樼骇鎶€宸э紝浠?VIP',
  `status`       TINYINT         NOT NULL DEFAULT 1 COMMENT '1鍙戝竷 0鑽夌',
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reading_slug` (`slug`),
  KEY `idx_reading_sort` (`status`, `sort_order`)
) ENGINE=InnoDB COMMENT='闃呰鎶€宸х珷鑺?;

CREATE TABLE `reading_passage` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `chapter_id`   BIGINT UNSIGNED NULL COMMENT '鎵€灞炵珷鑺傦紝鍙负绌鸿〃绀虹嫭绔嬬粌涔?,
  `title`        VARCHAR(200)    NOT NULL COMMENT '绡囩珷鏍囬',
  `content_en`   MEDIUMTEXT      NOT NULL COMMENT '鑻辨枃鍘熸枃',
  `word_count`   INT UNSIGNED    NULL COMMENT '璇嶆暟',
  `difficulty`   ENUM('cet4','cet6') NOT NULL DEFAULT 'cet4',
  `is_mock`      TINYINT         NOT NULL DEFAULT 1 COMMENT '1=鑷紪妯℃嫙棰?,
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
) ENGINE=InnoDB COMMENT='闃呰缁冧範绡囩珷';

CREATE TABLE `reading_question` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `passage_id`    BIGINT UNSIGNED NOT NULL,
  `question_type` ENUM('single','multiple','true_false') NOT NULL DEFAULT 'single',
  `stem`          TEXT            NOT NULL COMMENT '棰樺共',
  `explanation`   TEXT            NULL COMMENT '绛旀瑙ｆ瀽',
  `sort_order`    INT             NOT NULL DEFAULT 0,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rq_passage` (`passage_id`, `sort_order`),
  CONSTRAINT `fk_rq_passage` FOREIGN KEY (`passage_id`) REFERENCES `reading_passage` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='闃呰閫夋嫨棰?;

CREATE TABLE `reading_question_option` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `question_id` BIGINT UNSIGNED NOT NULL,
  `label`       CHAR(1)         NOT NULL COMMENT 'A/B/C/D',
  `content`     VARCHAR(512)    NOT NULL COMMENT '閫夐」鍐呭',
  `is_correct`  TINYINT         NOT NULL DEFAULT 0 COMMENT '1=姝ｇ‘绛旀',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_option_label` (`question_id`, `label`),
  CONSTRAINT `fk_rqo_question` FOREIGN KEY (`question_id`) REFERENCES `reading_question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='闃呰棰橀€夐」';

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
) ENGINE=InnoDB COMMENT='鐢ㄦ埛闃呰绔犺妭杩涘害';

-- -----------------------------------------------------------------------------
-- 6. 缈昏瘧妯″潡
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
) ENGINE=InnoDB COMMENT='缈昏瘧鎶€宸х珷鑺?;

CREATE TABLE `translation_question` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `chapter_id`        BIGINT UNSIGNED NULL,
  `direction`         ENUM('zh2en','en2zh') NOT NULL DEFAULT 'zh2en' COMMENT '缈昏瘧鏂瑰悜',
  `prompt_zh`         TEXT            NULL COMMENT '涓枃棰樺共锛堜腑璇戣嫳鏃剁敤锛?,
  `prompt_en`         TEXT            NULL COMMENT '鑻辨枃棰樺共锛堣嫳璇戜腑鏃剁敤锛?,
  `reference_answer`  TEXT            NOT NULL COMMENT '鍙傝€冪瓟妗?,
  `difficulty`        ENUM('cet4','cet6') NOT NULL DEFAULT 'cet4',
  `is_mock`           TINYINT         NOT NULL DEFAULT 1,
  `is_vip`            TINYINT         NOT NULL DEFAULT 0 COMMENT '1=浠?VIP 鍙粌',
  `source_id`         INT UNSIGNED    NULL,
  `sort_order`        INT             NOT NULL DEFAULT 0,
  `status`            TINYINT         NOT NULL DEFAULT 1,
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tq_chapter` (`chapter_id`),
  CONSTRAINT `fk_tq_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `translation_chapter` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_tq_source` FOREIGN KEY (`source_id`) REFERENCES `content_source` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='缈昏瘧缁冧範棰?;

CREATE TABLE `translation_submission` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT UNSIGNED NOT NULL,
  `question_id`     BIGINT UNSIGNED NOT NULL,
  `user_answer`     TEXT            NOT NULL COMMENT '鐢ㄦ埛璇戞枃',
  `score`           TINYINT UNSIGNED NULL COMMENT 'AI 璇勫垎 0-100',
  `overall_comment` VARCHAR(1000)   NULL COMMENT '鎬昏瘎',
  `errors_json`     JSON            NULL COMMENT '閿欒鐐?[{span,suggestion,reason}]',
  `reference_hint`  TEXT            NULL COMMENT '鍙傝€冭〃杈炬彁绀?,
  `ai_model`        VARCHAR(64)     NULL COMMENT '浣跨敤鐨勬ā鍨嬪悕',
  `ai_raw_response` JSON            NULL COMMENT 'LLM 鍘熷鍝嶅簲锛堣皟璇?瀹¤锛?,
  `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '1鎴愬姛 0澶辫触',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ts_user` (`user_id`, `created_at` DESC),
  KEY `idx_ts_question` (`question_id`),
  CONSTRAINT `fk_ts_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ts_question` FOREIGN KEY (`question_id`) REFERENCES `translation_question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='缈昏瘧鎻愪氦涓?AI 鎵规敼璁板綍';

-- -----------------------------------------------------------------------------
-- 7. 璇嶆眹妯″潡
-- -----------------------------------------------------------------------------

CREATE TABLE `vocabulary` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `word`         VARCHAR(64)     NOT NULL COMMENT '鍗曡瘝/鐭',
  `phonetic`     VARCHAR(64)     NULL COMMENT '闊虫爣',
  `meaning_zh`   VARCHAR(512)    NOT NULL COMMENT '涓枃閲婁箟',
  `example_en`   VARCHAR(512)    NULL,
  `example_zh`   VARCHAR(512)    NULL,
  `collocation`  VARCHAR(256)    NULL COMMENT '甯歌鎼厤',
  `difficulty`   ENUM('cet4','cet6') NOT NULL DEFAULT 'cet4',
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vocab_word` (`word`),
  KEY `idx_vocab_difficulty` (`difficulty`)
) ENGINE=InnoDB COMMENT='璇嶆眹搴?;

CREATE TABLE `sentence_pattern` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `pattern_en`  VARCHAR(512)    NOT NULL COMMENT '鍙ュ紡鑻辨枃',
  `pattern_zh`  VARCHAR(512)    NOT NULL COMMENT '鍙ュ紡涓枃',
  `example_en`  VARCHAR(512)    NULL,
  `example_zh`  VARCHAR(512)    NULL,
  `category`    VARCHAR(64)     NULL COMMENT '鍒嗙被锛氬啓浣?鍙ｈ/缈昏瘧',
  `difficulty`  ENUM('cet4','cet6') NOT NULL DEFAULT 'cet4',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pattern_category` (`category`)
) ENGINE=InnoDB COMMENT='鎼厤鍙ュ紡搴?;

CREATE TABLE `user_vocabulary` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `vocabulary_id` BIGINT UNSIGNED NOT NULL,
  `source_type` ENUM('manual','video','reading','translation') NOT NULL DEFAULT 'manual',
  `source_id`   BIGINT UNSIGNED NULL COMMENT '鏉ユ簮涓氬姟 ID',
  `note`        VARCHAR(512)    NULL COMMENT '鐢ㄦ埛绗旇',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uv_user_vocab` (`user_id`, `vocabulary_id`),
  CONSTRAINT `fk_uv_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_uv_vocab` FOREIGN KEY (`vocabulary_id`) REFERENCES `vocabulary` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='鐢ㄦ埛鐢熻瘝鏈?;

-- -----------------------------------------------------------------------------
-- 8. 鍙岃瑙嗛妯″潡
-- -----------------------------------------------------------------------------

CREATE TABLE `video_series` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title`       VARCHAR(200)    NOT NULL COMMENT '绯诲垪鍚嶏紝濡?TEco Lab',
  `description` VARCHAR(500)    NULL,
  `cover_url`   VARCHAR(512)    NULL,
  `sort_order`  INT             NOT NULL DEFAULT 0,
  `status`      TINYINT         NOT NULL DEFAULT 1,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='瑙嗛绯诲垪/涓撴爮';

CREATE TABLE `video` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `series_id`     BIGINT UNSIGNED NULL COMMENT '鎵€灞炵郴鍒?,
  `title`         VARCHAR(200)    NOT NULL,
  `description`   VARCHAR(1000)   NULL,
  `cover_url`     VARCHAR(512)    NULL,
  `storage_type`  ENUM('embed','oss') NOT NULL DEFAULT 'embed' COMMENT 'MVP=embed锛屽悗鏈?4K=oss',
  `provider`      ENUM('bilibili','youtube','self') NOT NULL DEFAULT 'bilibili',
  `source_url`    VARCHAR(512)    NOT NULL COMMENT 'B 绔欏師椤?URL',
  `embed_bvid`    VARCHAR(20)     NULL COMMENT 'BV 鍙凤紝濡?BV1qP4y1M7cb',
  `embed_aid`     BIGINT          NULL COMMENT 'B 绔?av/aid锛堝彲閫夛級',
  `embed_cid`     BIGINT          NULL COMMENT 'B 绔欏垎 P cid锛堝彲閫夛級',
  `play_url`      VARCHAR(512)    NULL COMMENT 'storage_type=oss 鏃剁殑 CDN 鍦板潃',
  `duration_sec`  INT UNSIGNED    NULL COMMENT '鏃堕暱锛堢锛?,
  `difficulty`    ENUM('easy','medium','hard') NOT NULL DEFAULT 'medium',
  `is_vip`        TINYINT         NOT NULL DEFAULT 0 COMMENT '1=涓撳睘瑙嗛',
  `view_count`    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '绔欏唴鎾斁璁℃暟',
  `sort_order`    INT             NOT NULL DEFAULT 0,
  `source_id`     INT UNSIGNED    NULL,
  `license_note`  VARCHAR(512)    NULL,
  `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1涓婃灦 0涓嬫灦',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_video_series` (`series_id`, `sort_order`),
  KEY `idx_video_vip` (`is_vip`, `status`),
  KEY `idx_video_bvid` (`embed_bvid`),
  CONSTRAINT `fk_video_series` FOREIGN KEY (`series_id`) REFERENCES `video_series` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_video_source` FOREIGN KEY (`source_id`) REFERENCES `content_source` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='鍙岃瑙嗛涓昏〃';

CREATE TABLE `video_sentence` (
  `id`        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `video_id`  BIGINT UNSIGNED NOT NULL,
  `seq`       INT UNSIGNED    NOT NULL COMMENT '鍙ュ瓙搴忓彿锛屼粠 1 寮€濮?,
  `start_ms`  INT UNSIGNED    NOT NULL COMMENT '寮€濮嬫绉?,
  `end_ms`    INT UNSIGNED    NOT NULL COMMENT '缁撴潫姣',
  `text_en`   TEXT            NOT NULL,
  `text_zh`   TEXT            NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vs_video_seq` (`video_id`, `seq`),
  KEY `idx_vs_video_time` (`video_id`, `start_ms`),
  CONSTRAINT `fk_vs_video` FOREIGN KEY (`video_id`) REFERENCES `video` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='瑙嗛鍙ョ骇瀛楀箷鏃堕棿杞?;

CREATE TABLE `video_favorite` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `video_id`   BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vf_user_video` (`user_id`, `video_id`),
  CONSTRAINT `fk_vf_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_vf_video` FOREIGN KEY (`video_id`) REFERENCES `video` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='鐢ㄦ埛鏀惰棌瑙嗛';

CREATE TABLE `user_video_progress` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`          BIGINT UNSIGNED NOT NULL,
  `video_id`         BIGINT UNSIGNED NOT NULL,
  `last_sentence_seq` INT UNSIGNED   NULL COMMENT '鏈€鍚庡涔犲埌鐨勫彞瀛愬簭鍙?,
  `last_position_ms`  INT UNSIGNED   NULL COMMENT '鏈€鍚庢挱鏀句綅缃?,
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uvp_user_video` (`user_id`, `video_id`),
  CONSTRAINT `fk_uvp_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_uvp_video` FOREIGN KEY (`video_id`) REFERENCES `video` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='鐢ㄦ埛瑙嗛瀛︿範杩涘害';

-- -----------------------------------------------------------------------------
-- 9. 绀惧尯 Feed
-- -----------------------------------------------------------------------------

CREATE TABLE `post` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `content`    TEXT            NOT NULL COMMENT '鏂囧瓧鍐呭',
  `like_count` INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '鍐椾綑璁℃暟',
  `status`     TINYINT         NOT NULL DEFAULT 1 COMMENT '1姝ｅ父 0闅愯棌 2瀹℃牳涓?,
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post_feed` (`status`, `created_at` DESC),
  KEY `idx_post_user` (`user_id`, `created_at` DESC),
  CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='绀惧尯甯栧瓙锛堜粎鏂囧瓧锛?;

CREATE TABLE `post_image` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `post_id`    BIGINT UNSIGNED NOT NULL,
  `file_id`    BIGINT UNSIGNED NOT NULL COMMENT 'file_asset.id',
  `sort_order` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_pi_post` (`post_id`, `sort_order`),
  CONSTRAINT `fk_pi_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pi_file` FOREIGN KEY (`file_id`) REFERENCES `file_asset` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='甯栧瓙鍥剧墖锛堝鍥撅級';

CREATE TABLE `post_like` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `post_id`    BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pl_user_post` (`user_id`, `post_id`),
  CONSTRAINT `fk_pl_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pl_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='甯栧瓙鐐硅禐';

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
) ENGINE=InnoDB COMMENT='甯栧瓙璇勮锛堢浜屾湡锛孧VP 鍙笉瀹炵幇 API锛?;

-- -----------------------------------------------------------------------------
-- 10. 绯荤粺
-- -----------------------------------------------------------------------------

CREATE TABLE `admin_audit_log` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `admin_id`    BIGINT UNSIGNED NULL COMMENT '鎿嶄綔浜?user.id',
  `action`      VARCHAR(64)     NOT NULL COMMENT '鎿嶄綔绫诲瀷',
  `target_type` VARCHAR(64)     NULL COMMENT '鐩爣瀹炰綋',
  `target_id`   BIGINT UNSIGNED NULL,
  `detail_json` JSON            NULL,
  `ip`          VARCHAR(45)     NULL,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_time` (`created_at` DESC)
) ENGINE=InnoDB COMMENT='鍚庡彴鎿嶄綔瀹¤';

-- -----------------------------------------------------------------------------
-- 11. 鍒濆绉嶅瓙锛堝彲閫夛紝寮€鍙戠幆澧冿級
-- -----------------------------------------------------------------------------

INSERT INTO `content_source` (`name`, `source_type`, `source_url`, `license_note`) VALUES
('ISLAND 鑷紪妯℃嫙棰?, 'original', NULL, '骞冲彴鍘熷垱锛岄潪鐪熼'),
('TED 瀹樻柟', 'ted', 'https://www.ted.com', 'Embed 鎾斁锛岄潪鍟嗙敤锛沄IP 鍟嗙敤闇€鍙﹁鎺堟潈');

INSERT INTO `vip_plan` (`code`, `name`, `price_cent`, `duration_days`) VALUES
('monthly', '鏈堝害 VIP', 1990, 30),
('yearly',  '骞村害 VIP', 9900, 365);
