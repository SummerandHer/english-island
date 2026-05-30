package com.island.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {

	private String token;
	private UserProfile user;

	@Data
	@Builder
	@AllArgsConstructor
	public static class UserProfile {
		private Long id;
		private String email;
		private String nickname;
		private String avatarUrl;
		private boolean vip;
		private LocalDateTime vipExpireAt;
		private String role;
		private boolean admin;
	}
}
