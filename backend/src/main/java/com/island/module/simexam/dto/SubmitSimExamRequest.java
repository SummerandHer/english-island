package com.island.module.simexam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SubmitSimExamRequest {

	@NotEmpty
	@Valid
	private List<AnswerItem> answers;

	/** 实际用时（秒），考试模式前端上报 */
	private Integer elapsedSeconds;

	@Getter
	@Setter
	public static class AnswerItem {

		@NotNull
		private Long questionId;

		@NotNull
		private String label;
	}
}
