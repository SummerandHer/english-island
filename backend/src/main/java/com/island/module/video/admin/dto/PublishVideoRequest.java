package com.island.module.video.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PublishVideoRequest {

	@NotBlank(message = "标题不能为空")
	private String title;

	@NotBlank(message = "封面不能为空")
	private String coverUrl;

	@NotBlank(message = "视频地址不能为空")
	private String videoObjectKey;

	private String playUrl;

	private String description;

	private Long seriesId;

	private String difficulty = "medium";

	private Integer isVip = 0;

	private Integer sortOrder = 0;

	/** 主题标签 ID 列表（可多选） */
	private List<Integer> tagIds;

	@NotNull(message = "发布状态不能为空")
	private Integer status = 1;

	@NotEmpty(message = "至少上传并解析出 1 句字幕")
	@Valid
	private List<SentenceDraft> sentences;

	@Data
	public static class SentenceDraft {
		@NotNull
		private Integer seq;
		@NotNull
		private Integer startMs;
		@NotNull
		private Integer endMs;
		@NotBlank
		private String textEn;
		private String textZh;
	}
}
