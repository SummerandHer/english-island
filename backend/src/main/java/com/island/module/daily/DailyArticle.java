package com.island.module.daily;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("daily_article")
public class DailyArticle {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String title;

	private String slug;

	private String topic;

	private String difficulty = "cet4";

	@TableField("content_en")
	private String contentEn;

	@TableField("cover_url")
	private String coverUrl;

	@TableField("cover_asset_id")
	private Long coverAssetId;

	@TableField("summary_zh")
	private String summaryZh;

	@TableField("publish_date")
	private LocalDate publishDate;

	@TableField("source_id")
	private Integer sourceId;

	@TableField("source_published_at")
	private LocalDate sourcePublishedAt;

	@TableField("source_author")
	private String sourceAuthor;

	@TableField("source_place")
	private String sourcePlace;

	@TableField("cet_vocab_json")
	private String cetVocabJson;

	@TableField("hard_vocab_json")
	private String hardVocabJson;

	@TableField("structures_json")
	private String structuresJson;

	@TableField("word_count")
	private Integer wordCount;

	private String status = "draft";

	@TableField("ai_raw_json")
	private String aiRawJson;

	/** idle | running | ok | failed | needs_review */
	@TableField("ai_status")
	private String aiStatus = "idle";

	@TableField("ai_error")
	private String aiError;

	@TableField("ai_version")
	private String aiVersion;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updatedAt;
}
