package com.island.module.vocabulary;

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
@TableName("user_vocabulary")
public class UserVocabulary {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("user_id")
	private Long userId;

	@TableField("vocabulary_id")
	private Long vocabularyId;

	@TableField("source_type")
	private String sourceType = "manual";

	@TableField("source_id")
	private Long sourceId;

	private String note;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;
}
