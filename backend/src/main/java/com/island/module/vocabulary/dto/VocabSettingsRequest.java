package com.island.module.vocabulary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VocabSettingsRequest {

	@NotBlank(message = "词书级别不能为空")
	@Pattern(regexp = "cet4|cet6", message = "词书级别须为 cet4 或 cet6")
	private String examLevel;
}
