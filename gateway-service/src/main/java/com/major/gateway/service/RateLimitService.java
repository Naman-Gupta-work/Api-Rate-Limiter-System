package com.major.gateway.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private final int LIMIT = 5;
    private final int WINDOW = 60; // seconds

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String apiKey) {

        String key = "rate_limit:" + apiKey;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            redisTemplate.expire(key, WINDOW, TimeUnit.SECONDS);
        }

        return count <= LIMIT;
    }
}