package com.island.module.video.admin.dto;

import lombok.Data;

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
}
