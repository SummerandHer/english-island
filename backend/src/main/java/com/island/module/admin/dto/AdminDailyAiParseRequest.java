package com.island.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDailyAiParseRequest {

	private String title;

	@NotBlank
	private String contentEn;

	/** @deprecated 保留兼容；enrich 不再依赖人工主题 */
	private String topic;

	/** @deprecated 保留兼容；enrich 不再依赖人工难度 */
	private String difficulty;
}
