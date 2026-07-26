package com.island.module.simexam;

import com.island.common.ApiResponse;
import com.island.module.simexam.dto.SubmitSimExamRequest;
import com.island.security.IslandUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sim-exam")
@RequiredArgsConstructor
public class SimExamController {

	private final SimExamService simExamService;

	@GetMapping("/hub")
	public ApiResponse<SimExamService.HubPayload> hub() {
		return ApiResponse.ok(simExamService.hub());
	}

	@GetMapping("/passages")
	public ApiResponse<List<SimExamService.PassageCard>> list(
			@RequestParam(required = false) String sectionType,
			@RequestParam(required = false) String examLevel) {
		return ApiResponse.ok(simExamService.listPublished(sectionType, examLevel));
	}

	@GetMapping("/passages/{id}")
	public ApiResponse<SimExamService.PracticeDetail> detail(@PathVariable Long id) {
		return ApiResponse.ok(simExamService.getPractice(id));
	}

	@PostMapping("/passages/{id}/submit")
	public ApiResponse<SimExamService.SubmitResult> submit(
			@PathVariable Long id,
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@Valid @RequestBody SubmitSimExamRequest request) {
		return ApiResponse.ok(simExamService.submit(id, userDetails.getUser().getId(), request));
	}

	@GetMapping("/submissions/mine")
	public ApiResponse<List<SimExamService.SubmissionSummary>> mine(
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@RequestParam(defaultValue = "20") int limit) {
		return ApiResponse.ok(simExamService.listMine(userDetails.getUser().getId(), limit));
	}
}
