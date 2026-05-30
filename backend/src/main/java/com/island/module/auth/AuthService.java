package com.island.module.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.island.common.BusinessException;
import com.island.module.auth.dto.AuthResponse;
import com.island.module.auth.dto.LoginRequest;
import com.island.module.auth.dto.RegisterRequest;
import com.island.module.auth.dto.ResetPasswordRequest;
import com.island.module.auth.dto.SendCodeRequest;
import com.island.module.user.User;
import com.island.module.user.mapper.UserMapper;
import com.island.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final VerificationCodeService verificationCodeService;
	private final EmailService emailService;

	public Map<String, Long> sendCode(SendCodeRequest request) {
		String email = QqEmailValidator.normalize(request.getEmail());
		VerificationCodeScene scene = parseScene(request.getScene());

		if (scene == VerificationCodeScene.REGISTER) {
			Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
			if (count != null && count > 0) {
				throw new BusinessException("该 QQ 邮箱已注册");
			}
		} else {
			User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
			if (user == null) {
				throw new BusinessException("该 QQ 邮箱尚未注册");
			}
		}

		String code = verificationCodeService.generateAndStore(email, scene);
		emailService.sendVerificationCode(email, scene, code);
		return Map.of("cooldownSeconds", verificationCodeService.getCooldownSeconds(email, scene));
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = QqEmailValidator.normalize(request.getEmail());
		verificationCodeService.consume(email, VerificationCodeScene.REGISTER, request.getCode());

		Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
		if (count != null && count > 0) {
			throw new BusinessException("该 QQ 邮箱已注册");
		}

		User user = new User();
		user.setEmail(email);
		user.setEmailVerified(true);
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		user.setNickname(request.getNickname() != null && !request.getNickname().isBlank()
				? request.getNickname().trim() : "岛民");
		userMapper.insert(user);
		return buildAuthResponse(user);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		String email = QqEmailValidator.normalize(request.getEmail());
		User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
		if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			throw new BusinessException(401, "邮箱或密码错误");
		}
		user.setLastLoginAt(LocalDateTime.now());
		userMapper.updateById(user);
		return buildAuthResponse(user);
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		String email = QqEmailValidator.normalize(request.getEmail());
		verificationCodeService.consume(email, VerificationCodeScene.RESET_PASSWORD, request.getCode());

		User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
		if (user == null) {
			throw new BusinessException("该 QQ 邮箱尚未注册");
		}
		user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		user.setEmailVerified(true);
		userMapper.updateById(user);
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

	private static VerificationCodeScene parseScene(String scene) {
		return switch (scene) {
			case "register" -> VerificationCodeScene.REGISTER;
			case "reset_password" -> VerificationCodeScene.RESET_PASSWORD;
			default -> throw new BusinessException("无效的场景");
		};
	}
}
