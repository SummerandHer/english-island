package com.island.module.vocabulary;

import com.island.common.ApiResponse;
import com.island.module.vocabulary.dto.AddUserVocabularyRequest;
import com.island.module.vocabulary.dto.NotebookReviewBatchRequest;
import com.island.module.vocabulary.dto.VocabReviewRequest;
import com.island.module.vocabulary.dto.VocabSettingsRequest;
import com.island.security.IslandUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

	@GetMapping("/stats")
	public ApiResponse<VocabularyService.VocabStats> stats(
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		return ApiResponse.ok(vocabularyService.getStats(userDetails.getUser().getId()));
	}

	@GetMapping("/settings")
	public ApiResponse<VocabularyService.VocabSettingsView> settings(
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		return ApiResponse.ok(vocabularyService.getSettings(userDetails.getUser().getId()));
	}

	@PutMapping("/settings")
	public ApiResponse<VocabularyService.VocabSettingsView> updateSettings(
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@Valid @RequestBody VocabSettingsRequest request) {
		return ApiResponse.ok(vocabularyService.updateSettings(userDetails.getUser().getId(), request));
	}

	@GetMapping("/checkin")
	public ApiResponse<VocabularyService.CheckinStats> checkin(
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		return ApiResponse.ok(vocabularyService.getCheckinStats(userDetails.getUser().getId()));
	}

	@GetMapping("/lookup")
	public ApiResponse<VocabularyService.VocabDetail> lookup(@RequestParam String word) {
		return ApiResponse.ok(vocabularyService.lookupByWord(word));
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
	public ApiResponse<List<VocabularyService.NotebookItem>> notebook(
			@AuthenticationPrincipal IslandUserDetails userDetails) {
		return ApiResponse.ok(vocabularyService.listNotebook(userDetails.getUser().getId()));
	}

	@PostMapping("/notebook/add-to-review")
	public ApiResponse<Map<String, Integer>> addNotebookToReview(
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@Valid @RequestBody NotebookReviewBatchRequest request) {
		int added = vocabularyService.addNotebookToReview(userDetails.getUser().getId(), request);
		return ApiResponse.ok(Map.of("added", added));
	}

	@PostMapping("/notebook")
	public ApiResponse<Void> addNotebook(
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@Valid @RequestBody AddUserVocabularyRequest request) {
		vocabularyService.addToNotebook(userDetails.getUser().getId(), request);
		return ApiResponse.ok(null);
	}
}
