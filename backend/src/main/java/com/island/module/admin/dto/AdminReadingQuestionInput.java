package com.island.module.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AdminReadingQuestionInput {

	@NotBlank(message = "题干不能为空")
	private String stem;

	private String explanation;

	private Integer sortOrder = 0;

	@NotEmpty(message = "至少需要 2 个选项")
	@Size(min = 2, max = 4, message = "选项数量为 2-4 个")
	@Valid
	private List<AdminReadingOptionInput> options;
}
