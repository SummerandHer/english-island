package com.island.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminReadingOptionInput {

	@NotBlank
	@Pattern(regexp = "[A-D]", message = "选项标签须为 A-D")
	private String label;

	@NotBlank(message = "选项内容不能为空")
	private String content;

	@NotNull(message = "请标记是否正确")
	private Integer isCorrect;
}
