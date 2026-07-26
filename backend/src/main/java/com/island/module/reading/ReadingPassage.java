package com.island.module.reading;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("reading_passage")
public class ReadingPassage {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("chapter_id")
	private Long chapterId;

	private String title;

	@TableField("content_en")
	private String contentEn;

	@TableField("long_sentences_json")
	private String longSentencesJson;

	@TableField("word_count")
	private Integer wordCount;

	private String difficulty = "cet4";

	@TableField("is_mock")
	private Integer isMock = 1;

	@TableField("source_id")
	private Integer sourceId;

	@TableField("sort_order")
	private Integer sortOrder = 0;

	private Integer status = 1;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updatedAt;
}
