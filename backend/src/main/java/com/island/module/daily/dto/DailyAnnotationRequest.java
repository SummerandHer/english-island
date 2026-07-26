package com.island.module.daily.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DailyAnnotationRequest {

	@NotNull
	@Min(0)
	private Integer startOffset;

	@NotNull
	@Min(0)
	private Integer endOffset;

	@NotBlank
	@Size(max = 2000)
	private String selectedText;

	@Size(max = 16)
	private String color;

	@Size(max = 500)
	private String note;
}
