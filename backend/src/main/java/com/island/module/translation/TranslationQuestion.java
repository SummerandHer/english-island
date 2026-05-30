package com.island.module.translation;

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
@TableName("translation_question")
public class TranslationQuestion {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("chapter_id")
	private Long chapterId;

	private String direction = "zh2en";

	@TableField("prompt_zh")
	private String promptZh;

	@TableField("prompt_en")
	private String promptEn;

	@TableField("reference_answer")
	private String referenceAnswer;

	private String difficulty = "cet4";

	@TableField("is_mock")
	private Integer isMock = 1;

	@TableField("is_vip")
	private Integer isVip = 0;

	@TableField("sort_order")
	private Integer sortOrder = 0;

	private Integer status = 1;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updatedAt;

	public TranslationDirection getDirectionEnum() {
		return TranslationDirection.valueOf(direction);
	}

	public enum TranslationDirection { zh2en, en2zh }
}
