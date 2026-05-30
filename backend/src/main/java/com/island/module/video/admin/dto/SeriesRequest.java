package com.island.module.video.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SeriesRequest {
	@NotBlank
	private String title;
	private String description;
	private String coverUrl;
	private Integer sortOrder = 0;
	private Integer status = 1;
}
