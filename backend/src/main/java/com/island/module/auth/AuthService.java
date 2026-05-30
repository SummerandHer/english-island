package com.island.module.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.island.common.BusinessException;
import com.island.module.auth.dto.AuthResponse;
import com.island.module.auth.dto.LoginRequest;
import com.island.module.auth.dto.RegisterRequest;
import com.island.module.user.User;
import com.island.module.user.mapper.UserMapper;
import com.island.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
				.eq(User::getEmail, request.getEmail()));
		if (count != null && count > 0) {
			throw new BusinessException("邮箱已注册");
		}
		User user = new User();
		user.setEmail(request.getEmail());
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		user.setNickname(request.getNickname() != null && !request.getNickname().isBlank()
				? request.getNickname() : "岛民");
		userMapper.insert(user);
		return buildAuthResponse(user);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
				.eq(User::getEmail, request.getEmail()));
		if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			throw new BusinessException(401, "邮箱或密码错误");
		}
		user.setLastLoginAt(LocalDateTime.now());
		userMapper.updateById(user);
		return buildAuthResponse(user);
	}

	public AuthResponse.UserProfile getProfile(User user) {
		return toProfile(user);
	}

	private AuthResponse buildAuthResponse(User user) {
		String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());
		return AuthResponse.builder()
				.token(token)
				.user(toProfile(user))
				.build();
	}

	private AuthResponse.UserProfile toProfile(User user) {
		return AuthResponse.UserProfile.builder()
				.id(user.getId())
				.email(user.getEmail())
				.nickname(user.getNickname())
				.avatarUrl(user.getAvatarUrl())
				.vip(user.isVipActive())
				.vipExpireAt(user.getVipExpireAt())
				.build();
	}
}
