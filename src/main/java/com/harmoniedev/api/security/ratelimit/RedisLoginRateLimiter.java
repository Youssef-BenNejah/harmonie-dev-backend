package com.harmoniedev.api.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app", name = "mode", havingValue = "cloud", matchIfMissing = true)
public class RedisLoginRateLimiter implements LoginRateLimiter {
	private final ProxyManager<byte[]> proxyManager;
	private final BucketConfiguration configuration;

	public RedisLoginRateLimiter(ProxyManager<byte[]> proxyManager) {
		this.proxyManager = proxyManager;
		this.configuration = BucketConfiguration.builder()
				.addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
				.build();
	}

	@Override
	public boolean tryConsume(String key) {
		byte[] bucketKey = key.getBytes(StandardCharsets.UTF_8);
		Bucket bucket = proxyManager.builder().build(bucketKey, configuration);
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
		return probe.isConsumed();
	}
}
