package com.island.module.translation;

import com.island.common.ApiResponse;
import com.island.module.translation.dto.SubmitTranslationRequest;
import com.island.security.IslandUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/translation")
@RequiredArgsConstructor
public class TranslationController {

	private final TranslationService translationService;

	@GetMapping("/chapters")
	public ApiResponse<List<TranslationService.ChapterSummary>> listChapters() {
		return ApiResponse.ok(translationService.listChapters());
	}

	@GetMapping("/chapters/{slug}")
	public ApiResponse<TranslationService.ChapterDetail> chapterDetail(
			@PathVariable String slug,
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		return ApiResponse.ok(translationService.getChapter(slug, userDetails));
	}

	@GetMapping("/questions")
	public ApiResponse<List<TranslationService.QuestionSummary>> listQuestions() {
		return ApiResponse.ok(translationService.listQuestions());
	}

	@GetMapping("/questions/{id}")
	public ApiResponse<TranslationService.QuestionDetail> questionDetail(
			@PathVariable Long id,
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		return ApiResponse.ok(translationService.getQuestion(id, userDetails));
	}

	@PostMapping("/submissions")
	public ApiResponse<TranslationService.SubmissionResult> submit(
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@Valid @RequestBody SubmitTranslationRequest request) {
		return ApiResponse.ok(translationService.submit(userDetails.getUser().getId(), request));
	}
}
