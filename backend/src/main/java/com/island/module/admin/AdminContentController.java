package com.island.module.admin;

import com.island.common.ApiResponse;
import com.island.module.admin.dto.AdminDailyAiParseRequest;
import com.island.module.admin.dto.AdminDailyArticleRequest;
import com.island.module.admin.dto.AdminReadingPassageRequest;
import com.island.module.admin.dto.AdminTranslationQuestionRequest;
import com.island.module.daily.DailyService;
import com.island.module.simexam.SimExamService;
import com.island.module.simexam.dto.AdminSimSourceRequest;
import com.island.module.simexam.dto.SubmitSimExamRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/content")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminContentController {

	private final AdminContentService adminContentService;
	private final DailyService dailyService;
	private final SimExamService simExamService;

	@GetMapping("/translation/chapters")
	public ApiResponse<List<AdminContentService.ChapterOption>> translationChapters() {
		return ApiResponse.ok(adminContentService.listTranslationChapters());
	}

	@GetMapping("/translation/questions")
	public ApiResponse<List<AdminContentService.AdminTranslationSummary>> listTranslationQuestions() {
		return ApiResponse.ok(adminContentService.listTranslationQuestions());
	}

	@GetMapping("/translation/questions/{id}")
	public ApiResponse<AdminContentService.AdminTranslationDetail> getTranslationQuestion(@PathVariable Long id) {
		return ApiResponse.ok(adminContentService.getTranslationQuestion(id));
	}

	@PostMapping("/translation/questions")
	public ApiResponse<Map<String, Long>> createTranslationQuestion(
			@Valid @RequestBody AdminTranslationQuestionRequest request) {
		Long id = adminContentService.createTranslationQuestion(request);
		return ApiResponse.ok(Map.of("id", id));
	}

	@PutMapping("/translation/questions/{id}")
	public ApiResponse<Void> updateTranslationQuestion(
			@PathVariable Long id,
			@Valid @RequestBody AdminTranslationQuestionRequest request) {
		adminContentService.updateTranslationQuestion(id, request);
		return ApiResponse.ok(null);
	}

	@PatchMapping("/translation/questions/{id}/status")
	public ApiResponse<Void> translationQuestionStatus(
			@PathVariable Long id,
			@RequestParam int status) {
		adminContentService.setTranslationQuestionStatus(id, status);
		return ApiResponse.ok(null);
	}

	@GetMapping("/reading/chapters")
	public ApiResponse<List<AdminContentService.ChapterOption>> readingChapters() {
		return ApiResponse.ok(adminContentService.listReadingChapters());
	}

	@GetMapping("/reading/passages")
	public ApiResponse<List<AdminContentService.AdminPassageSummary>> listReadingPassages() {
		return ApiResponse.ok(adminContentService.listReadingPassages());
	}

	@GetMapping("/reading/passages/{id}")
	public ApiResponse<AdminContentService.AdminPassageDetail> getReadingPassage(@PathVariable Long id) {
		return ApiResponse.ok(adminContentService.getReadingPassage(id));
	}

	@PostMapping("/reading/passages")
	public ApiResponse<Map<String, Long>> createReadingPassage(
			@Valid @RequestBody AdminReadingPassageRequest request) {
		Long id = adminContentService.createReadingPassage(request);
		return ApiResponse.ok(Map.of("id", id));
	}

	@PutMapping("/reading/passages/{id}")
	public ApiResponse<Void> updateReadingPassage(
			@PathVariable Long id,
			@Valid @RequestBody AdminReadingPassageRequest request) {
		adminContentService.updateReadingPassage(id, request);
		return ApiResponse.ok(null);
	}

	@PatchMapping("/reading/passages/{id}/status")
	public ApiResponse<Void> readingPassageStatus(
			@PathVariable Long id,
			@RequestParam int status) {
		adminContentService.setReadingPassageStatus(id, status);
		return ApiResponse.ok(null);
	}

	@GetMapping("/daily/articles")
	public ApiResponse<List<DailyService.AdminArticleSummary>> listDailyArticles(
			@RequestParam(required = false) String status) {
		return ApiResponse.ok(dailyService.adminList(status));
	}

	@GetMapping("/daily/articles/{id}")
	public ApiResponse<DailyService.AdminArticleDetail> getDailyArticle(@PathVariable Long id) {
		return ApiResponse.ok(dailyService.adminGet(id));
	}

	@PostMapping("/daily/articles")
	public ApiResponse<Map<String, Long>> createDailyArticle(
			@Valid @RequestBody AdminDailyArticleRequest request) {
		Long id = dailyService.adminCreate(request);
		return ApiResponse.ok(Map.of("id", id));
	}

	@PutMapping("/daily/articles/{id}")
	public ApiResponse<Void> updateDailyArticle(
			@PathVariable Long id,
			@Valid @RequestBody AdminDailyArticleRequest request) {
		dailyService.adminUpdate(id, request);
		return ApiResponse.ok(null);
	}

	@PatchMapping("/daily/articles/{id}/status")
	public ApiResponse<Void> dailyArticleStatus(
			@PathVariable Long id,
			@RequestParam String status) {
		dailyService.adminSetStatus(id, status);
		return ApiResponse.ok(null);
	}

	@PostMapping("/daily/ai-parse")
	public ApiResponse<Map<String, Object>> dailyAiParse(
			@Valid @RequestBody AdminDailyAiParseRequest request) {
		return ApiResponse.ok(dailyService.adminAiParse(
				request.getTitle(),
				request.getContentEn(),
				request.getTopic(),
				request.getDifficulty()));
	}

	@PostMapping("/daily/ai-enrich")
	public ApiResponse<com.island.module.daily.dto.DailyAiEnrichmentResult> dailyAiEnrich(
			@Valid @RequestBody AdminDailyAiParseRequest request) {
		return ApiResponse.ok(dailyService.adminAiEnrich(request.getTitle(), request.getContentEn()));
	}

	@PostMapping("/daily/articles/{id}/ai-enrich")
	public ApiResponse<com.island.module.daily.dto.DailyAiEnrichmentResult> dailyAiEnrichArticle(
			@PathVariable Long id) {
		return ApiResponse.ok(dailyService.adminAiEnrichArticle(id));
	}

	@GetMapping("/sim-exam/sources")
	public ApiResponse<List<SimExamService.SourceSummary>> listSimSources() {
		return ApiResponse.ok(simExamService.adminListSources());
	}

	@GetMapping("/sim-exam/sources/{id}")
	public ApiResponse<SimExamService.SourceDetail> getSimSource(@PathVariable Long id) {
		return ApiResponse.ok(simExamService.adminGetSource(id));
	}

	@PostMapping("/sim-exam/sources")
	public ApiResponse<Map<String, Long>> createSimSource(@Valid @RequestBody AdminSimSourceRequest request) {
		return ApiResponse.ok(Map.of("id", simExamService.adminCreateSource(request)));
	}

	@PutMapping("/sim-exam/sources/{id}")
	public ApiResponse<Void> updateSimSource(
			@PathVariable Long id,
			@Valid @RequestBody AdminSimSourceRequest request) {
		simExamService.adminUpdateSource(id, request);
		return ApiResponse.ok(null);
	}

	@PostMapping("/sim-exam/sources/{id}/generate")
	public ApiResponse<SimExamService.GenerateResult> generateSim(@PathVariable Long id) {
		return ApiResponse.ok(simExamService.adminGenerate(id));
	}

	@GetMapping("/sim-exam/passages")
	public ApiResponse<List<SimExamService.AdminPassageSummary>> listSimPassages(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String sectionType) {
		return ApiResponse.ok(simExamService.adminListPassages(status, sectionType));
	}

	@GetMapping("/sim-exam/passages/{id}")
	public ApiResponse<SimExamService.AdminPassageDetail> getSimPassage(@PathVariable Long id) {
		return ApiResponse.ok(simExamService.adminGetPassage(id));
	}

	@PostMapping("/sim-exam/passages/{id}/publish")
	public ApiResponse<Void> publishSimPassage(@PathVariable Long id) {
		simExamService.adminPublish(id);
		return ApiResponse.ok(null);
	}

	@PostMapping("/sim-exam/passages/{id}/trial-submit")
	public ApiResponse<SimExamService.SubmitResult> trialSubmitSim(
			@PathVariable Long id,
			@Valid @RequestBody SubmitSimExamRequest request) {
		return ApiResponse.ok(simExamService.adminTrialSubmit(id, request));
	}
}
