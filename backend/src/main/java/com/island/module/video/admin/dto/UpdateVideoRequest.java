package com.island.module.video.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateVideoRequest {
	private String title;
	private String description;
	private String coverUrl;
	private Long seriesId;
	private String difficulty;
	private Integer isVip;
	private Integer sortOrder;
	private Integer status;
	/** 传入则全量替换主题标签；null 表示不改 */
	private List<Integer> tagIds;
}
