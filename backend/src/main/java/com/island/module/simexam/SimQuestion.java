package com.island.module.simexam;

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
@TableName("sim_question")
public class SimQuestion {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("passage_id")
	private Long passageId;

	@TableField("question_type")
	private String questionType = "single";

	private String stem;

	@TableField("skill_tag")
	private String skillTag;

	@TableField("answer_key")
	private String answerKey;

	@TableField("locate_en")
	private String locateEn;

	@TableField("locate_zh")
	private String locateZh;

	@TableField("explain_correct")
	private String explainCorrect;

	@TableField("explain_distractors_json")
	private String explainDistractorsJson;

	@TableField("explain_tip")
	private String explainTip;

	@TableField("sort_order")
	private Integer sortOrder = 0;

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;
}
