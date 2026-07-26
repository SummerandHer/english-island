package com.island.module.simexam;

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
@TableName(value = "sim_submission", autoResultMap = true)
public class SimSubmission {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("user_id")
	private Long userId;

	@TableField("passage_id")
	private Long passageId;

	@TableField("correct_count")
	private Integer correctCount;

	@TableField("total_questions")
	private Integer totalQuestions;

	@TableField("elapsed_seconds")
	private Integer elapsedSeconds = 0;

	@TableField(value = "answers_json", typeHandler = JacksonTypeHandler.class)
	private List<Map<String, Object>> answersJson;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;
}
