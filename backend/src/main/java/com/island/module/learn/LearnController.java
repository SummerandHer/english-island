package com.island.module.learn;

import com.island.common.ApiResponse;
import com.island.security.IslandUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learn")
@RequiredArgsConstructor
public class LearnController {

	private final LearnService learnService;

	@GetMapping("/summary")
	public ApiResponse<LearnService.LearnSummary> summary(
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		return ApiResponse.ok(learnService.getSummary(userDetails.getUser().getId()));
	}
}
