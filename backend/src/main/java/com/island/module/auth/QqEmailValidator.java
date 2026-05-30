package com.island.module.auth;

import java.util.Locale;
import java.util.regex.Pattern;

public final class QqEmailValidator {

	private static final Pattern QQ_EMAIL = Pattern.compile("^[a-zA-Z0-9._%+-]+@qq\\.com$");

	private QqEmailValidator() {
	}

	public static boolean isValid(String email) {
		if (email == null || email.isBlank()) {
			return false;
		}
		return QQ_EMAIL.matcher(email.trim().toLowerCase(Locale.ROOT)).matches();
	}

	public static String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
