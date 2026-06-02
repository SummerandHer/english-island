package com.island.module.reading;

import com.island.common.ApiResponse;
import com.island.module.reading.dto.SubmitReadingRequest;
import com.island.security.IslandUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reading")
@RequiredArgsConstructor
public class ReadingController {

	private final ReadingService readingService;
	private final ReadingPassageService passageService;

	@GetMapping("/chapters")
	public ApiResponse<List<ReadingService.ReadingChapterSummary>> list() {
		return ApiResponse.ok(readingService.listChapters());
	}

	@GetMapping("/chapters/{slug}")
	public ApiResponse<ReadingService.ReadingChapterDetail> detail(
			@PathVariable String slug,
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		return ApiResponse.ok(readingService.getChapter(slug, userDetails));
	}

	@GetMapping("/passages/{id}")
	public ApiResponse<ReadingPassageService.PassagePracticeDetail> passageDetail(@PathVariable Long id) {
		return ApiResponse.ok(passageService.getPracticeDetail(id));
	}

	@PostMapping("/passages/{id}/submit")
	public ApiResponse<ReadingPassageService.SubmitResult> submitPassage(
			@PathVariable Long id,
			@Valid @RequestBody SubmitReadingRequest request) {
		return ApiResponse.ok(passageService.submit(id, request));
	}
}
