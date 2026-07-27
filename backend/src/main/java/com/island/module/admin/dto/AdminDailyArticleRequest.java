package com.island.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AdminDailyArticleRequest {

	@NotBlank
	@Size(max = 200)
	private String title;

	@Size(max = 120)
	private String slug;

	/** draft 可空，ready/published 必填；由 AI 生成 */
	@Size(max = 40)
	private String topic;

	/** draft 可空，ready/published 必填；由 AI 生成 */
	private String difficulty;

	@NotBlank
	private String contentEn;

	@Size(max = 512)
	private String coverUrl;

	private Long coverAssetId;

	@Size(max = 800)
	private String summaryZh;

	@NotNull
	private LocalDate publishDate;

	private Integer sourceId;

	private LocalDate sourcePublishedAt;

	@Size(max = 128)
	private String sourceAuthor;

	@Size(max = 128)
	private String sourcePlace;

	private String cetVocabJson;

	private String hardVocabJson;

	private String structuresJson;

	private String contentZh;

	private String sentencesJson;

	private Integer wordCount;

	/** draft | ready | published */
	@Size(max = 16)
	private String status;
}
