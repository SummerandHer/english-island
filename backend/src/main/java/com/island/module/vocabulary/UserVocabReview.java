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
@TableName("user_vocab_review")
public class UserVocabReview {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("user_id")
	private Long userId;

	@TableField("vocabulary_id")
	private Long vocabularyId;

	private Integer familiarity = 0;

	@TableField("next_review_at")
	private LocalDateTime nextReviewAt;

	@TableField("last_review_at")
	private LocalDateTime lastReviewAt;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updatedAt;
}
