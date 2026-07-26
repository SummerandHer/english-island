package com.island.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminTranslationQuestionRequest {

	private Long chapterId;

	@NotBlank(message = "中文题干不能为空")
	private String promptZh;

	@NotBlank(message = "参考答案不能为空")
	private String referenceAnswer;

	private String difficulty = "cet4";

	private Integer isMock = 1;

	private Integer isVip = 0;

	private Integer sortOrder = 0;

	private Integer status = 1;
}
