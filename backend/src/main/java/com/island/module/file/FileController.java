package com.island.module.file;

import com.island.common.ApiResponse;
import com.island.common.PageResult;
import com.island.module.file.dto.FileDto;
import com.island.security.IslandUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

	private final FileService fileService;

	@PostMapping
	public ApiResponse<FileDto> upload(
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@RequestParam("file") MultipartFile file) {
		return ApiResponse.ok(fileService.upload(userDetails.getUser().getId(), file));
	}

	@GetMapping("/{id}")
	public ApiResponse<FileDto> get(@PathVariable Long id) {
		return ApiResponse.ok(fileService.getById(id));
	}

	@GetMapping
	public ApiResponse<PageResult<FileDto>> list(
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(fileService.listByUser(userDetails.getUser().getId(), page, size));
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@PathVariable Long id) {
		fileService.delete(userDetails.getUser().getId(), id);
		return ApiResponse.ok(null);
	}
}
