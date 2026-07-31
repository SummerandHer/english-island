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
@TableName("sim_source_paper")
public class SimSourcePaper {

	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("exam_level")
	private String examLevel = "cet4";

	@TableField("section_type")
	private String sectionType;

	private String title;

	@TableField("passage_en")
	private String passageEn;

	/** 结构化题目（供 AI 生成），由粘贴文本解析得到 */
	@TableField("questions_json")
	private String questionsJson;

	/** 管理员粘贴的题目原文，原样回显 */
	@TableField("questions_text")
	private String questionsText;

	/** 管理员填写的答案原文，原样回显 */
	@TableField("answers_text")
	private String answersText;

	@TableField("source_meta")
	private String sourceMeta;

	@TableField("license_note")
	private String licenseNote;

	@TableField("official_explains")
	private String officialExplains;

	private String status = "draft";

	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updatedAt;
}
