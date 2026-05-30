package com.island.module.auth;

import com.island.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class VerificationCodeService {

	private static final Duration CODE_TTL = Duration.ofMinutes(5);
	private static final Duration COOLDOWN = Duration.ofSeconds(60);
	private static final SecureRandom RANDOM = new SecureRandom();

	private final VerificationCodeStore store;

	public String generateAndStore(String email, VerificationCodeScene scene) {
		String normalizedEmail = QqEmailValidator.normalize(email);
		String cooldownKey = cooldownKey(scene, normalizedEmail);
		if (store.hasKey(cooldownKey)) {
			throw new BusinessException("验证码发送过于频繁，请稍后再试");
		}

		String code = String.format("%06d", RANDOM.nextInt(1_000_000));
		store.set(codeKey(scene, normalizedEmail), code, CODE_TTL);
		store.set(cooldownKey, "1", COOLDOWN);
		return code;
	}

	public void verify(String email, VerificationCodeScene scene, String code) {
		if (code == null || code.isBlank()) {
			throw new BusinessException("请输入验证码");
		}
		String normalizedEmail = QqEmailValidator.normalize(email);
		String stored = store.get(codeKey(scene, normalizedEmail));
		if (stored == null) {
			throw new BusinessException("验证码已过期，请重新获取");
		}
		if (!stored.equals(code.trim())) {
			throw new BusinessException("验证码错误");
		}
	}

	public void consume(String email, VerificationCodeScene scene, String code) {
		verify(email, scene, code);
		String normalizedEmail = QqEmailValidator.normalize(email);
		store.delete(codeKey(scene, normalizedEmail));
	}

	public long getCooldownSeconds(String email, VerificationCodeScene scene) {
		String normalizedEmail = QqEmailValidator.normalize(email);
		return store.getExpireSeconds(cooldownKey(scene, normalizedEmail));
	}

	private static String codeKey(VerificationCodeScene scene, String email) {
		return "island:code:" + scene.name().toLowerCase() + ":" + email;
	}

	private static String cooldownKey(VerificationCodeScene scene, String email) {
		return "island:code:cooldown:" + scene.name().toLowerCase() + ":" + email;
	}
}
