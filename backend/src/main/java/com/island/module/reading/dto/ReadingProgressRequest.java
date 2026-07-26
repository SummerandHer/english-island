package com.island.module.reading.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReadingProgressRequest {

	@NotNull(message = "请指定是否学完")
	private Boolean finished;
}
