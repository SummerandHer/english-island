package com.island.module.video;

import com.island.common.ApiResponse;
import com.island.common.PageResult;
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
	public ApiResponse<PageResult<VideoService.VideoSummary>> list(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "12") int size,
			@RequestParam(required = false) String tag,
			@RequestParam(defaultValue = "all") String filter,
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		Long userId = userDetails != null ? userDetails.getUser().getId() : null;
		return ApiResponse.ok(videoService.listVideos(page, size, userId, tag, filter));
	}

	@GetMapping("/overview")
	public ApiResponse<VideoService.VideoOverview> overview(
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		Long userId = userDetails != null ? userDetails.getUser().getId() : null;
		return ApiResponse.ok(videoService.overview(userId));
	}

	@GetMapping("/tags")
	public ApiResponse<List<VideoService.TagDto>> tags() {
		return ApiResponse.ok(videoService.listActiveTags());
	}

	@GetMapping("/{id}")
	public ApiResponse<VideoService.VideoDetail> detail(
			@PathVariable Long id,
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		return ApiResponse.ok(videoService.getVideo(id, userDetails));
	}

	@PostMapping("/{id}/favorite")
	public ApiResponse<Map<String, Boolean>> toggleFavorite(
			@PathVariable Long id,
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		videoService.toggleFavorite(userDetails.getUser().getId(), id);
		return ApiResponse.ok(Map.of("ok", true));
	}

	@PostMapping("/{id}/progress")
	public ApiResponse<Map<String, Boolean>> progress(
			@PathVariable Long id,
			@RequestBody(required = false) ProgressRequest body,
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		Integer seq = body != null ? body.lastSentenceSeq() : null;
		Integer pos = body != null ? body.lastPositionMs() : null;
		videoService.touchProgress(userDetails.getUser().getId(), id, seq, pos);
		return ApiResponse.ok(Map.of("ok", true));
	}

	public record ProgressRequest(Integer lastSentenceSeq, Integer lastPositionMs) {}
}
