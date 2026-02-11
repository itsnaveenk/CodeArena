package com.codearena.util;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class TokenBucketRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int maxRequests, Duration window) {
        TokenBucket bucket = buckets.computeIfAbsent(key,
            k -> new TokenBucket(maxRequests, window));

        bucket.updateConfig(maxRequests, window);

        return bucket.tryConsume();
    }

    @Override
    public long getRetryAfterSeconds(String key) {
        TokenBucket bucket = buckets.get(key);
        return bucket != null ? bucket.getSecondsUntilRefill() : 0;
    }

    public void clear() {
        buckets.clear();
    }

    private static class TokenBucket {
        private int maxTokens;
        private long windowMillis;
        private final AtomicLong tokens;
        private final AtomicLong windowStart;

        TokenBucket(int maxTokens, Duration window) {
            this.maxTokens = maxTokens;
            this.windowMillis = window.toMillis();
            this.tokens = new AtomicLong(maxTokens);
            this.windowStart = new AtomicLong(System.currentTimeMillis());
        }

        void updateConfig(int maxTokens, Duration window) {
            this.maxTokens = maxTokens;
            this.windowMillis = window.toMillis();
        }

        boolean tryConsume() {
            long now = System.currentTimeMillis();
            long currentWindowStart = windowStart.get();

            if (now - currentWindowStart >= windowMillis) {
                if (windowStart.compareAndSet(currentWindowStart, now)) {
                    tokens.set(maxTokens);
                }
            }

            while (true) {
                long currentTokens = tokens.get();
                if (currentTokens <= 0) {
                    return false;
                }
                if (tokens.compareAndSet(currentTokens, currentTokens - 1)) {
                    return true;
                }
            }
        }

        long getSecondsUntilRefill() {
            long now = System.currentTimeMillis();
            long currentWindowStart = windowStart.get();
            long elapsed = now - currentWindowStart;

            if (elapsed >= windowMillis) {
                return 0;
            }

            long remaining = windowMillis - elapsed;
            return (remaining + 999) / 1000;
        }
    }
}
