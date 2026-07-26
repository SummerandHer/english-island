package com.island.module.vocabulary.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddUserVocabularyRequest {

	@NotNull(message = "词汇 ID 不能为空")
	private Long vocabularyId;

	private String note;

	private String sourceType;

	private Long sourceId;
}
