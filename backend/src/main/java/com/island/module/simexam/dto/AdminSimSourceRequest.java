package com.island.module.simexam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSimSourceRequest {

	@NotBlank
	private String examLevel;

	@NotBlank
	private String sectionType;

	@NotBlank
	private String title;

	@NotBlank
	private String passageEn;

	/**
	 * 优先：粘贴的题目纯文本（46. 题干 + A) 选项）。
	 * 兼容：仍可传 JSON 数组。
	 * 若为空则回退 {@link #questionsJson}。
	 */
	private String questionsText;

	/** 可选答案：{@code 46.D 47.A} 或按题序 {@code D A D C B} */
	private String answersText;

	/** 兼容旧前端字段 */
	private String questionsJson;

	private String sourceMeta;

	private String licenseNote;

	private String officialExplains;
}
