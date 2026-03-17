package com.major.gateway.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RateLimitService {

    private final Map<String, Integer> requestCounts = new HashMap<>();
    private final Map<String, Long> timestamps = new HashMap<>();

    private final int LIMIT = 5; // requests per minute
    private final long WINDOW = 60 * 1000; // 1 minute

    public boolean isAllowed(String apiKey) {

        long currentTime = System.currentTimeMillis();

        timestamps.putIfAbsent(apiKey, currentTime);
        requestCounts.putIfAbsent(apiKey, 0);

        long startTime = timestamps.get(apiKey);

        // Reset window after 1 minute
        if (currentTime - startTime > WINDOW) {
            timestamps.put(apiKey, currentTime);
            requestCounts.put(apiKey, 0);
        }

        int count = requestCounts.get(apiKey);

        if (count >= LIMIT) {
            return false;
        }

        requestCounts.put(apiKey, count + 1);

        return true;
    }
}
