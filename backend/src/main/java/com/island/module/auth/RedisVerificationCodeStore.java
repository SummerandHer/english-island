package com.island.module.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class RedisVerificationCodeStore implements VerificationCodeStore {

	private final StringRedisTemplate redis;

	@Override
	public boolean hasKey(String key) {
		return Boolean.TRUE.equals(redis.hasKey(key));
	}

	@Override
	public void set(String key, String value, Duration ttl) {
		redis.opsForValue().set(key, value, ttl);
	}

	@Override
	public String get(String key) {
		return redis.opsForValue().get(key);
	}

	@Override
	public void delete(String key) {
		redis.delete(key);
	}

	@Override
	public long getExpireSeconds(String key) {
		Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
		return ttl != null && ttl > 0 ? ttl : 0;
	}
}
