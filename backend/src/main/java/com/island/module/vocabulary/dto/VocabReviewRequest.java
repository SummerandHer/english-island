package com.island.module.vocabulary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VocabReviewRequest {

	@NotNull(message = "词汇 ID 不能为空")
	private Long vocabularyId;

	@NotBlank(message = "请选择复习结果")
	@Pattern(regexp = "know|vague|unknown", message = "结果须为 know、vague 或 unknown")
	private String result;
}
