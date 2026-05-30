package com.island.module.post;

import com.island.common.ApiResponse;
import com.island.common.PageResult;
import com.island.security.IslandUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@GetMapping
	public ApiResponse<PageResult<PostService.PostDto>> feed(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(postService.listFeed(page, size));
	}

	@PostMapping
	public ApiResponse<PostService.PostDto> create(
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@RequestBody PostService.CreatePostRequest request) {
		return ApiResponse.ok(postService.createPost(userDetails.getUser().getId(), request));
	}

	@PostMapping("/upload")
	public ApiResponse<PostService.FileUploadResult> upload(
			@AuthenticationPrincipal IslandUserDetails userDetails,
			@RequestParam("file") MultipartFile file) {
		return ApiResponse.ok(postService.uploadImage(userDetails.getUser().getId(), file));
	}
}
