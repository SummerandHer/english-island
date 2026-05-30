package com.island.module.video;

import com.island.common.ApiResponse;
import com.island.security.IslandUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

	private final VideoService videoService;

	@GetMapping
	public ApiResponse<List<VideoService.VideoSummary>> list(
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		Long userId = userDetails != null ? userDetails.getUser().getId() : null;
		return ApiResponse.ok(videoService.listVideos(userId));
	}

	@GetMapping("/{id}")
	public ApiResponse<VideoService.VideoDetail> detail(
			@PathVariable Long id,
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		Long userId = userDetails != null ? userDetails.getUser().getId() : null;
		return ApiResponse.ok(videoService.getVideo(id, userId));
	}

	@PostMapping("/{id}/favorite")
	public ApiResponse<Map<String, Boolean>> toggleFavorite(
			@PathVariable Long id,
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		videoService.toggleFavorite(userDetails.getUser().getId(), id);
		return ApiResponse.ok(Map.of("ok", true));
	}
}
