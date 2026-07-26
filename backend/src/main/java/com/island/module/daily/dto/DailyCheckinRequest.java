package com.island.module.daily.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DailyCheckinRequest {

	@Min(0)
	private int readSeconds;
}
