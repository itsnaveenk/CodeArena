package com.codearena.util;

import java.time.Duration;

public interface RateLimiter {

    boolean tryAcquire(String key, int maxRequests, Duration window);

    long getRetryAfterSeconds(String key);
}
