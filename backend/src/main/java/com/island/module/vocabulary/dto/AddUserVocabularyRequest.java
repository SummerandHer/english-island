package com.island.module.vocabulary.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddUserVocabularyRequest {

	/** 学习词库 ID；≤0 或缺省时用 word 从查词词典升格 */
	private Long vocabularyId;

	/** 点词原文，dict 命中升格时必填 */
	private String word;

	private String note;

	private String sourceType;

	private Long sourceId;
}
