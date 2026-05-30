package com.island.module.translation;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@TableName(value = "translation_submission", autoResultMap = true)
public class TranslationSubmission {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("user_id")
	private Long userId;

	@TableField("question_id")
	private Long questionId;

	@TableField("user_answer")
	private String userAnswer;

	private Integer score;

	@TableField("overall_comment")
	private String overallComment;

	@TableField(value = "errors_json", typeHandler = JacksonTypeHandler.class)
	private List<Map<String, String>> errorsJson;

	@TableField("reference_hint")
	private String referenceHint;

	@TableField("ai_model")
	private String aiModel;

	@TableField(value = "ai_raw_response", typeHandler = JacksonTypeHandler.class)
	private Map<String, Object> aiRawResponse;

	private Integer status = 1;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;
}
