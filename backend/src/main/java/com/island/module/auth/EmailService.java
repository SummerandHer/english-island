package com.island.module.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

	private final JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String from;

	public void sendVerificationCode(String to, VerificationCodeScene scene, String code) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(to);
		message.setSubject(buildSubject(scene));
		message.setText(buildBody(scene, code));
		mailSender.send(message);
	}

	private static String buildSubject(VerificationCodeScene scene) {
		return switch (scene) {
			case REGISTER -> "【四六级岛 ISLAND】注册验证码";
			case RESET_PASSWORD -> "【四六级岛 ISLAND】重置密码验证码";
		};
	}

	private static String buildBody(VerificationCodeScene scene, String code) {
		String action = switch (scene) {
			case REGISTER -> "注册账号";
			case RESET_PASSWORD -> "重置密码";
		};
		return """
				你好，

				你正在%s，验证码为：%s

				验证码 5 分钟内有效，请勿泄露给他人。如非本人操作，请忽略此邮件。

				—— 四六级岛 ISLAND
				""".formatted(action, code);
	}
}
