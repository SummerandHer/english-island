package com.island.module.video.admin;

import com.island.common.ApiResponse;
import com.island.common.PageResult;
import com.island.module.file.FileService;
import com.island.module.file.dto.FileDto;
import com.island.module.video.VideoSeries;
import com.island.module.video.VideoService;
import com.island.module.video.admin.dto.*;
import com.island.security.IslandUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminVideoController {

	private final AdminVideoService adminVideoService;
	private final FileService fileService;
	private final VideoService videoService;

	@PostMapping("/videos/parse")
	public ApiResponse<ParseVideoResponse> parse(
			@AuthenticationPrincipal IslandUserDetails user,
			@RequestParam("file") MultipartFile file,
			@RequestParam(defaultValue = "true") boolean generateZh) {
		return ApiResponse.ok(adminVideoService.parseUpload(user.getUser().getId(), file, generateZh));
	}

	@PostMapping("/videos")
	public ApiResponse<Map<String, Long>> publish(
			@AuthenticationPrincipal IslandUserDetails user,
			@Valid @RequestBody PublishVideoRequest request) {
		Long id = adminVideoService.publish(user.getUser().getId(), request);
		return ApiResponse.ok(Map.of("id", id));
	}

	@GetMapping("/videos")
	public ApiResponse<PageResult<AdminVideoSummary>> list(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) Integer status) {
		return ApiResponse.ok(adminVideoService.list(page, size, status));
	}

	@GetMapping("/videos/{id}")
	public ApiResponse<AdminVideoDetail> detail(@PathVariable Long id) {
		return ApiResponse.ok(adminVideoService.getDetail(id));
	}

	@PutMapping("/videos/{id}")
	public ApiResponse<Void> update(@PathVariable Long id, @RequestBody UpdateVideoRequest request) {
		adminVideoService.update(id, request);
		return ApiResponse.ok(null);
	}

	@PutMapping("/videos/{id}/sentences")
	public ApiResponse<Void> updateSentences(
			@PathVariable Long id,
			@Valid @RequestBody UpdateSentencesRequest request) {
		adminVideoService.updateSentences(id, request);
		return ApiResponse.ok(null);
	}

	@PostMapping("/videos/{id}/zh-draft")
	public ApiResponse<List<ParseVideoResponse.SentenceDraft>> zhDraft(@PathVariable Long id) {
		return ApiResponse.ok(adminVideoService.generateZhDraft(id));
	}

	@PostMapping("/files/cover")
	public ApiResponse<FileDto> uploadCover(
			@AuthenticationPrincipal IslandUserDetails user,
			@RequestParam("file") MultipartFile file) {
		return ApiResponse.ok(fileService.upload(user.getUser().getId(), file, "images"));
	}

	@GetMapping("/video-tags")
	public ApiResponse<List<VideoService.TagDto>> listTags() {
		return ApiResponse.ok(videoService.listActiveTags());
	}

	@GetMapping("/video-series")
	public ApiResponse<List<VideoSeries>> listSeries() {
		return ApiResponse.ok(adminVideoService.listSeries());
	}

	@PostMapping("/video-series")
	public ApiResponse<Map<String, Long>> createSeries(@Valid @RequestBody SeriesRequest request) {
		return ApiResponse.ok(Map.of("id", adminVideoService.createSeries(request)));
	}

	@PutMapping("/video-series/{id}")
	public ApiResponse<Void> updateSeries(@PathVariable Long id, @Valid @RequestBody SeriesRequest request) {
		adminVideoService.updateSeries(id, request);
		return ApiResponse.ok(null);
	}
}
