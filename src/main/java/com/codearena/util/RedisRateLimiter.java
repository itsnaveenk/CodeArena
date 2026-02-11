package com.codearena.util;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Primary
@Component
public class RedisRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "rate-limit:";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryAcquire(String key, int maxRequests, Duration window) {
        String redisKey = KEY_PREFIX + key;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - window.toMillis();

        try {
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

            Long currentCount = redisTemplate.opsForZSet().count(redisKey, windowStart, now);

            if (currentCount != null && currentCount < maxRequests) {
                redisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);
                redisTemplate.expire(redisKey, window.toMillis() + 5000, TimeUnit.MILLISECONDS);
                return true;
            }

            return false;
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public long getRetryAfterSeconds(String key) {
        String redisKey = KEY_PREFIX + key;

        try {
            var oldest = redisTemplate.opsForZSet().range(redisKey, 0, 0);

            if (oldest != null && !oldest.isEmpty()) {
                String oldestEntry = (String) oldest.iterator().next();
                long oldestTimestamp = Long.parseLong(oldestEntry);
                long now = Instant.now().toEpochMilli();
                long diff = oldestTimestamp - now;

                return Math.max(0, diff / 1000);
            }

            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
