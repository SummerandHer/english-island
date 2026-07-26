package com.island.module.daily;

import com.island.common.ApiResponse;
import com.island.module.daily.dto.DailyAnnotationRequest;
import com.island.module.daily.dto.DailyCheckinRequest;
import com.island.security.IslandUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/daily")
@RequiredArgsConstructor
public class DailyController {

	private final DailyService dailyService;

	@GetMapping("/topics")
	public ApiResponse<List<Map<String, String>>> topics() {
		return ApiResponse.ok(dailyService.topics());
	}

	@GetMapping("/hub")
	public ApiResponse<DailyService.HubPayload> hub(
			@RequestParam(required = false) String topic,
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		return ApiResponse.ok(dailyService.hub(topic, userDetails));
	}

	@GetMapping("/articles/{id}")
	public ApiResponse<DailyService.ArticleDetail> detail(
			@PathVariable Long id,
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		return ApiResponse.ok(dailyService.getPublishedDetail(id, userDetails));
	}

	@PostMapping("/articles/{id}/checkin")
	public ApiResponse<Void> checkin(
			@PathVariable Long id,
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@RequestBody(required = false) DailyCheckinRequest request) {
		dailyService.checkin(id, userDetails.getUser().getId(), request);
		return ApiResponse.ok(null);
	}

	@GetMapping("/articles/{id}/annotations")
	public ApiResponse<List<DailyService.AnnotationView>> listAnnotations(
			@PathVariable Long id,
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		return ApiResponse.ok(dailyService.listAnnotations(id, userDetails.getUser().getId()));
	}

	@PostMapping("/articles/{id}/annotations")
	public ApiResponse<DailyService.AnnotationView> createAnnotation(
			@PathVariable Long id,
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@Valid @RequestBody DailyAnnotationRequest request) {
		return ApiResponse.ok(dailyService.createAnnotation(id, userDetails.getUser().getId(), request));
	}

	@DeleteMapping("/articles/{id}/annotations/{annotationId}")
	public ApiResponse<Void> deleteAnnotation(
			@PathVariable Long id,
			@PathVariable Long annotationId,
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		dailyService.deleteAnnotation(id, annotationId, userDetails.getUser().getId());
		return ApiResponse.ok(null);
	}
}
