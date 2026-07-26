package com.island.module.simexam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSimSourceRequest {

	@NotBlank
	private String examLevel;

	@NotBlank
	private String sectionType;

	@NotBlank
	private String title;

	@NotBlank
	private String passageEn;

	@NotBlank
	private String questionsJson;

	private String sourceMeta;

	private String licenseNote;

	private String officialExplains;
}
