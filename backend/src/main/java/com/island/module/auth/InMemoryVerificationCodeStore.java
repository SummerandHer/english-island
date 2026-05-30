package com.island.module.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Primary
@ConditionalOnProperty(name = "island.verification.store", havingValue = "memory")
public class InMemoryVerificationCodeStore implements VerificationCodeStore {

	private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

	@Override
	public boolean hasKey(String key) {
		return getAlive(key) != null;
	}

	@Override
	public void set(String key, String value, Duration ttl) {
		cache.put(key, new CacheEntry(value, System.currentTimeMillis() + ttl.toMillis()));
	}

	@Override
	public String get(String key) {
		CacheEntry entry = getAlive(key);
		return entry != null ? entry.value : null;
	}

	@Override
	public void delete(String key) {
		cache.remove(key);
	}

	@Override
	public long getExpireSeconds(String key) {
		CacheEntry entry = getAlive(key);
		if (entry == null) {
			return 0;
		}
		long remainMs = entry.expireAtMs - System.currentTimeMillis();
		return remainMs > 0 ? (remainMs + 999) / 1000 : 0;
	}

	private CacheEntry getAlive(String key) {
		CacheEntry entry = cache.get(key);
		if (entry == null) {
			return null;
		}
		if (entry.expireAtMs <= System.currentTimeMillis()) {
			cache.remove(key);
			return null;
		}
		return entry;
	}

	private record CacheEntry(String value, long expireAtMs) {
	}
}
