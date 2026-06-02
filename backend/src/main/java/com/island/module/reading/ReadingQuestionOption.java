package com.island.module.reading;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("reading_question_option")
public class ReadingQuestionOption {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("question_id")
	private Long questionId;

	private String label;

	private String content;

	@TableField("is_correct")
	private Integer isCorrect = 0;
}
