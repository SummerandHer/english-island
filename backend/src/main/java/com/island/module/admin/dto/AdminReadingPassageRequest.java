package com.island.module.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AdminReadingPassageRequest {

	private Long chapterId;

	@NotBlank(message = "标题不能为空")
	private String title;

	@NotBlank(message = "英文正文不能为空")
	private String contentEn;

	private String difficulty = "cet4";

	private Integer isMock = 1;

	private Integer sortOrder = 0;

	private Integer status = 1;

	@NotEmpty(message = "至少包含 1 道题")
	@Valid
	private List<AdminReadingQuestionInput> questions;
}
