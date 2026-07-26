package com.island.module.simexam;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sim_option")
public class SimOption {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("question_id")
	private Long questionId;

	private String label;

	private String content;

	@TableField("is_correct")
	private Integer isCorrect = 0;
}
