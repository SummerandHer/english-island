package com.island.module.auth;

import java.time.Duration;

public interface VerificationCodeStore {

	boolean hasKey(String key);

	void set(String key, String value, Duration ttl);

	String get(String key);

	void delete(String key);

	long getExpireSeconds(String key);
}
