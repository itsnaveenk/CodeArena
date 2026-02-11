package com.codearena.util;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

/**
 * Property-based tests for RateLimiter.
 */
@Tag("Feature: codearena-platform")
class RateLimiterProperties {

    /**
     * Property 26: Rate Limiting Enforcement
     * For any user making N requests within a minute window:
     * - Run endpoint: if N > 10, subsequent requests rejected with 429
     * - Submit endpoint: if N > 5, subsequent requests rejected with 429
     * - Different users have independent rate limits
     *
     * **Validates: Requirements 15.1, 15.2, 15.3, 15.4**
     */
    @Property(tries = 100)
    @Tag("Property 26: Rate Limiting Enforcement")
    void rateLimitingEnforcement(
        @ForAll @IntRange(min = 1, max = 20) int requestCount,
        @ForAll("userIds") String userId
    ) {
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter();
        int maxRequests = 10;
        Duration window = Duration.ofMinutes(1);
        String key = "run:" + userId;

        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < requestCount; i++) {
            if (rateLimiter.tryAcquire(key, maxRequests, window)) {
                successCount++;
            } else {
                failCount++;
            }
        }

        // First maxRequests should succeed, rest should fail
        assertThat(successCount).isEqualTo(Math.min(requestCount, maxRequests));
        assertThat(failCount).isEqualTo(Math.max(0, requestCount - maxRequests));
    }

    @Property(tries = 100)
    @Tag("Property 26a: Run Endpoint Rate Limit (10/min)")
    void runEndpointRateLimit(@ForAll("userIds") String userId) {
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter();
        String key = "run:" + userId;
        int maxRequests = 10;
        Duration window = Duration.ofMinutes(1);

        // First 10 requests should succeed
        for (int i = 0; i < 10; i++) {
            assertThat(rateLimiter.tryAcquire(key, maxRequests, window))
                .as("Request %d should succeed", i + 1)
                .isTrue();
        }

        // 11th request should fail
        assertThat(rateLimiter.tryAcquire(key, maxRequests, window))
            .as("Request 11 should fail")
            .isFalse();

        // Retry-after should be positive
        assertThat(rateLimiter.getRetryAfterSeconds(key))
            .as("Retry-after should be positive")
            .isGreaterThan(0);
    }

    @Property(tries = 100)
    @Tag("Property 26b: Submit Endpoint Rate Limit (5/min)")
    void submitEndpointRateLimit(@ForAll("userIds") String userId) {
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter();
        String key = "submit:" + userId;
        int maxRequests = 5;
        Duration window = Duration.ofMinutes(1);

        // First 5 requests should succeed
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.tryAcquire(key, maxRequests, window))
                .as("Request %d should succeed", i + 1)
                .isTrue();
        }

        // 6th request should fail
        assertThat(rateLimiter.tryAcquire(key, maxRequests, window))
            .as("Request 6 should fail")
            .isFalse();
    }

    @Property(tries = 100)
    @Tag("Property 26c: Independent User Rate Limits")
    void independentUserRateLimits(
        @ForAll("userIds") String userId1,
        @ForAll("userIds") String userId2
    ) {
        Assume.that(!userId1.equals(userId2));

        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter();
        int maxRequests = 5;
        Duration window = Duration.ofMinutes(1);

        String key1 = "submit:" + userId1;
        String key2 = "submit:" + userId2;

        // Exhaust user1's rate limit
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire(key1, maxRequests, window);
        }

        // User1 should be rate limited
        assertThat(rateLimiter.tryAcquire(key1, maxRequests, window))
            .as("User1 should be rate limited")
            .isFalse();

        // User2 should still have full quota
        assertThat(rateLimiter.tryAcquire(key2, maxRequests, window))
            .as("User2 should not be affected by User1's rate limit")
            .isTrue();
    }

    @Property(tries = 100)
    @Tag("Property 26d: Retry-After Header Value")
    void retryAfterHeaderValue(@ForAll("userIds") String userId) {
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter();
        String key = "run:" + userId;
        int maxRequests = 10;
        Duration window = Duration.ofMinutes(1);

        // Exhaust rate limit
        for (int i = 0; i < 10; i++) {
            rateLimiter.tryAcquire(key, maxRequests, window);
        }

        // Verify rate limit exceeded
        assertThat(rateLimiter.tryAcquire(key, maxRequests, window)).isFalse();

        // Retry-after should be between 1 and 60 seconds
        long retryAfter = rateLimiter.getRetryAfterSeconds(key);
        assertThat(retryAfter)
            .as("Retry-after should be between 1 and 60 seconds")
            .isBetween(1L, 60L);
    }

    @Property(tries = 50)
    @Tag("Property 26e: Rate Limit Resets After Window")
    void rateLimitResetsAfterWindow(@ForAll("userIds") String userId) throws InterruptedException {
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter();
        String key = "test:" + userId;
        int maxRequests = 3;
        Duration window = Duration.ofMillis(100); // Short window for testing

        // Exhaust rate limit
        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryAcquire(key, maxRequests, window)).isTrue();
        }
        assertThat(rateLimiter.tryAcquire(key, maxRequests, window)).isFalse();

        // Wait for window to expire
        Thread.sleep(150);

        // Should be able to make requests again
        assertThat(rateLimiter.tryAcquire(key, maxRequests, window))
            .as("Rate limit should reset after window expires")
            .isTrue();
    }

    @Provide
    Arbitrary<String> userIds() {
        return Arbitraries.longs().between(1, 10000).map(String::valueOf);
    }
}
