package com.island.module.reading;

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
@TableName(value = "reading_submission", autoResultMap = true)
public class ReadingSubmission {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("user_id")
	private Long userId;

	@TableField("passage_id")
	private Long passageId;

	@TableField("chapter_id")
	private Long chapterId;

	@TableField("correct_count")
	private Integer correctCount;

	@TableField("total_questions")
	private Integer totalQuestions;

	@TableField(value = "answers_json", typeHandler = JacksonTypeHandler.class)
	private List<Map<String, Object>> answersJson;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;
}
