package com.island.module.reading;

import com.island.common.ApiResponse;
import com.island.module.reading.dto.ReadingProgressRequest;
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
	private final ReadingSubmissionService submissionService;

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

	@PostMapping("/chapters/{slug}/progress")
	public ApiResponse<Void> saveProgress(
			@PathVariable String slug,
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@Valid @RequestBody ReadingProgressRequest request) {
		readingService.saveChapterProgress(userDetails.getUser().getId(), slug, request);
		return ApiResponse.ok(null);
	}

	@GetMapping("/passages/{id}")
	public ApiResponse<ReadingPassageService.PassagePracticeDetail> passageDetail(@PathVariable Long id) {
		return ApiResponse.ok(passageService.getPracticeDetail(id));
	}

	@PostMapping("/passages/{id}/submit")
	public ApiResponse<ReadingPassageService.SubmitResult> submitPassage(
			@PathVariable Long id,
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@Valid @RequestBody SubmitReadingRequest request) {
		ReadingPassageService.SubmitResult result = passageService.submit(id, request);
		if (userDetails != null) {
			ReadingPassage passage = passageService.getPassageEntity(id);
			submissionService.saveSubmission(userDetails.getUser().getId(), passage, result);
		}
		return ApiResponse.ok(result);
	}

	@GetMapping("/submissions/mine")
	public ApiResponse<List<ReadingSubmissionService.SubmissionSummary>> mySubmissions(
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@RequestParam(defaultValue = "20") int limit) {
		return ApiResponse.ok(submissionService.listMine(userDetails.getUser().getId(), limit));
	}

	@GetMapping("/submissions/last-scores")
	public ApiResponse<java.util.Map<Long, ReadingSubmissionService.PassageLastScore>> lastScores(
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@RequestParam List<Long> passageIds) {
		return ApiResponse.ok(submissionService.lastScoresByPassage(userDetails.getUser().getId(), passageIds));
	}
}
