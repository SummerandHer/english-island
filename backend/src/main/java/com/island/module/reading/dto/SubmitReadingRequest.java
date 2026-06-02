package com.island.module.reading.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SubmitReadingRequest {

	@NotEmpty(message = "请至少提交一道题的答案")
	@Valid
	private List<AnswerItem> answers;

	@Getter
	@Setter
	public static class AnswerItem {

		@NotNull(message = "题目 ID 不能为空")
		private Long questionId;

		@NotNull(message = "选项不能为空")
		private String label;
	}
}
