package com.island.module.translation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitTranslationRequest {

	@NotNull
	private Long questionId;

	@NotBlank
	private String userAnswer;
}
