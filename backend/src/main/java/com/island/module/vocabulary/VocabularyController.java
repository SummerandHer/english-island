package com.island.module.vocabulary;

import com.island.common.ApiResponse;
import com.island.module.vocabulary.dto.AddUserVocabularyRequest;
import com.island.module.vocabulary.dto.VocabReviewRequest;
import com.island.security.IslandUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vocabulary")
@RequiredArgsConstructor
public class VocabularyController {

	private final VocabularyService vocabularyService;

	@GetMapping("/today")
	public ApiResponse<VocabularyService.TodayPlan> today(
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		return ApiResponse.ok(vocabularyService.getTodayPlan(userDetails.getUser().getId()));
	}

	@GetMapping("/search")
	public ApiResponse<List<VocabularyService.VocabListItem>> search(@RequestParam String q) {
		return ApiResponse.ok(vocabularyService.search(q));
	}

	@GetMapping("/{id}")
	public ApiResponse<VocabularyService.VocabDetail> detail(@PathVariable Long id) {
		return ApiResponse.ok(vocabularyService.getFullDetail(id));
	}

	@PostMapping("/review")
	public ApiResponse<VocabularyService.ReviewResult> review(
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@Valid @RequestBody VocabReviewRequest request) {
		return ApiResponse.ok(vocabularyService.review(userDetails.getUser().getId(), request));
	}

	@GetMapping("/notebook/mine")
	public ApiResponse<List<VocabularyService.VocabListItem>> notebook(
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		return ApiResponse.ok(vocabularyService.listNotebook(userDetails.getUser().getId()));
	}

	@PostMapping("/notebook")
	public ApiResponse<Void> addNotebook(
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@Valid @RequestBody AddUserVocabularyRequest request) {
		vocabularyService.addToNotebook(userDetails.getUser().getId(), request);
		return ApiResponse.ok(null);
	}
}
