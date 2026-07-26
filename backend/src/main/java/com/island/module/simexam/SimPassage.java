package com.island.module.simexam;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("sim_passage")
public class SimPassage {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("derived_from_source_id")
	private Long derivedFromSourceId;

	@TableField("exam_level")
	private String examLevel = "cet4";

	@TableField("section_type")
	private String sectionType;

	private String title;

	@TableField("content_en")
	private String contentEn;

	@TableField("content_zh")
	private String contentZh;

	@TableField("word_count")
	private Integer wordCount;

	@TableField("vocab_count")
	private Integer vocabCount;

	@TableField("recommended_minutes")
	private Integer recommendedMinutes;

	@TableField("vocab_json")
	private String vocabJson;

	private String status = "draft";

	@TableField("ai_raw_json")
	private String aiRawJson;

	@TableField("ai_status")
	private String aiStatus = "idle";

	@TableField("ai_error")
	private String aiError;

	@TableField("ai_version")
	private String aiVersion;

	@TableField("similarity_score")
	private BigDecimal similarityScore;

	@TableField("sort_order")
	private Integer sortOrder = 0;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updatedAt;
}
