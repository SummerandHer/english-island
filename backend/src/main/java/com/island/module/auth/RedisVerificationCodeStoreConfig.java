package com.island.module.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnProperty(name = "island.verification.store", havingValue = "redis", matchIfMissing = true)
public class RedisVerificationCodeStoreConfig {

	@Bean
	@Primary
	public VerificationCodeStore redisVerificationCodeStore(StringRedisTemplate redis) {
		return new RedisVerificationCodeStore(redis);
	}
}
