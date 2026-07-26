package com.island.module.vocabulary.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NotebookReviewBatchRequest {

	@NotEmpty(message = "词汇 ID 列表不能为空")
	private List<Long> vocabularyIds;
}
