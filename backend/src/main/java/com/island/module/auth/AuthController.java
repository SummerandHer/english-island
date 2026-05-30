package com.island.module.auth;

import com.island.common.ApiResponse;
import com.island.module.auth.dto.AuthResponse;
import com.island.module.auth.dto.LoginRequest;
import com.island.module.auth.dto.RegisterRequest;
import com.island.module.user.User;
import com.island.security.IslandUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ApiResponse.ok(authService.register(request));
	}

	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.ok(authService.login(request));
	}

	@GetMapping("/me")
	public ApiResponse<AuthResponse.UserProfile> me(@AuthenticationPrincipal IslandUserDetails userDetails) {
		return ApiResponse.ok(authService.getProfile(userDetails.getUser()));
	}
}
