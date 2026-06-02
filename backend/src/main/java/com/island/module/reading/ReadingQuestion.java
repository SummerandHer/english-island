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
@TableName("reading_question")
public class ReadingQuestion {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("passage_id")
	private Long passageId;

	@TableField("question_type")
	private String questionType = "single";

	private String stem;

	private String explanation;

	@TableField("sort_order")
	private Integer sortOrder = 0;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;
}
