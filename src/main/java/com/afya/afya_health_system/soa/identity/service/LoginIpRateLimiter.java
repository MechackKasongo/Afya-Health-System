package com.afya.afya_health_system.soa.identity.service;

import com.afya.afya_health_system.soa.identity.config.LoginRateLimitProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Per-IP Bucket4j buckets cached with Caffeine (idle-eviction avoids unbounded memory).
 * <p>Français : seaux token par IP, mis en cache avec expiration d’accès.</p>
 */
@Component
public class LoginIpRateLimiter {

    private final LoginRateLimitProperties properties;
    private final Cache<String, Bucket> bucketsByClientKey;

    public LoginIpRateLimiter(LoginRateLimitProperties properties) {
        this.properties = properties;
        this.bucketsByClientKey =
                Caffeine.newBuilder().maximumSize(50_000).expireAfterAccess(Duration.ofHours(4)).build();
    }

    /** @return {@code true} si la tentative est autorisée, {@code false} si le quota IP est dépassé */
    public boolean tryAcquire(String clientKey) {
        if (!properties.isEnabled()) {
            return true;
        }
        int cap = Math.max(1, properties.getRequestsPerMinute());
        int minutes = Math.max(1, properties.getRefillDurationMinutes());
        Bucket bucket = bucketsByClientKey.get(clientKey, k -> newBucket(cap, minutes));
        return bucket.tryConsume(1);
    }

    private static Bucket newBucket(int capacity, int refillMinutes) {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(capacity, Duration.ofMinutes(refillMinutes)))
                .build();
    }
}
