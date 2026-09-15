package com.harmoniedev.api.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {
	@Value("${spring.data.redis.host}")
	private String redisHost;

	@Value("${spring.data.redis.port}")
	private int redisPort;

	@Value("${spring.data.redis.password:}")
	private String redisPassword;

	@Bean(destroyMethod = "shutdown")
	public RedisClient bucket4jRedisClient() {
		RedisURI.Builder builder = RedisURI.builder()
				.withHost(redisHost)
				.withPort(redisPort);
		if (redisPassword != null && !redisPassword.isBlank()) {
			builder.withPassword(redisPassword.toCharArray());
		}
		return RedisClient.create(builder.build());
	}

	@Bean(destroyMethod = "close")
	public StatefulRedisConnection<byte[], byte[]> bucket4jRedisConnection(RedisClient client) {
		return client.connect(ByteArrayCodec.INSTANCE);
	}

	@Bean
	public ProxyManager<byte[]> bucket4jProxyManager(StatefulRedisConnection<byte[], byte[]> connection) {
		return LettuceBasedProxyManager.builderFor(connection).build();
	}
}
