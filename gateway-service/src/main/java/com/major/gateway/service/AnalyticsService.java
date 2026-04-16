package com.major.gateway.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class AnalyticsService {

    // The "Cabinet": Maps a "UserId:Path" string to a map of Timestamps
    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, ChartPoint>> storage = new ConcurrentHashMap<>();

    private final MeterRegistry registry;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public AnalyticsService(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Logic: When a request happens, we record it twice.
     * 1. Once for the specific path (e.g., "User1:/test")
     * 2. Once for the user's total (e.g., "User1:all")
     */
    public void recordRequest(Long userId, String path, int status, long latencyMs) {
        long second = System.currentTimeMillis() / 1000;

        // Use a unique key per user to prevent data mixing
        String specificKey = userId + ":" + path;
        String globalKey = userId + ":all";

        // Update Lifetime Totals (Micrometer)
        registry.counter("gateway.requests", "user", userId.toString(), "path", path, "status", String.valueOf(status)).increment();
        registry.timer("gateway.latency", "user", userId.toString(), "path", path).record(latencyMs, TimeUnit.MILLISECONDS);

        // Update Chart Data (Sliding Window)
        updateBucket(specificKey, second, status, latencyMs);
        updateBucket(globalKey, second, status, latencyMs);
    }

    private void updateBucket(String key, long second, int status, long latencyMs) {
        // Get the "Folder" for this specific User:Path
        ConcurrentHashMap<Long, ChartPoint> timeMap = storage.computeIfAbsent(key, k -> new ConcurrentHashMap<>());

        // Get the "Sheet" for this exact second
        ChartPoint point = timeMap.computeIfAbsent(second, k -> new ChartPoint());

        if (status >= 400) {
            point.blocked++;
        } else {
            point.allowed++;
            point.totalLatency += latencyMs;
        }
    }

    public Map<String, Object> getDashboardData(Long userId, String path) {
        long now = System.currentTimeMillis() / 1000;
        String lookupKey = userId + ":" + (path == null ? "all" : path);

        // 1. Get Lifetime Totals from the "Odometer" (Registry)
        double total = getMicrometerCount(userId, path);
        double blocked = getMicrometerBlocked(userId, path);
        double avgLat = getMicrometerLatency(userId, path);

        // 2. Get Last 60 Seconds for the Chart
        List<Map<String, Object>> chartData = new ArrayList<>();
        ConcurrentHashMap<Long, ChartPoint> timeMap = storage.getOrDefault(lookupKey, new ConcurrentHashMap<>());

        for (long i = now - 60; i <= now; i++) {
            ChartPoint p = timeMap.getOrDefault(i, new ChartPoint());
            chartData.add(Map.of(
                    "time", timeFormatter.format(Instant.ofEpochSecond(i)),
                    "allowed", p.allowed,
                    "blocked", p.blocked,
                    "latency", p.allowed > 0 ? p.totalLatency / p.allowed : 0
            ));
        }

        // Cleanup memory (delete data older than 2 mins)
        storage.values().forEach(map -> map.keySet().removeIf(t -> t < now - 120));

        return Map.of(
                "summary", Map.of(
                        "totalRequests", (long)total,
                        "blockedRequests", (long)blocked ,
                "requestsPerSec", String.format("%.2f", total / 60.0),
                "avgLatencyMs", (long)avgLat
        ),
        "chartData", chartData
        );
    }

    // --- MICROMETER HELPERS (The Odometer Readers) ---

    private double getMicrometerCount(Long userId, String path) {
        try {
            var search = registry.find("gateway.requests").tag("user", userId.toString());
            if (path != null && !path.equals("all")) search = search.tag("path", path);
            return search.counters().stream().mapToDouble(io.micrometer.core.instrument.Counter::count).sum();
        } catch (Exception e) { return 0; }
    }

    private double getMicrometerBlocked(Long userId, String path) {
        try {
            var search = registry.find("gateway.requests").tag("user", userId.toString());
            if (path != null && !path.equals("all")) search = search.tag("path", path);
            return search.counters().stream()
                .filter(c -> {
                    String statusStr = c.getId().getTag("status");
                    if (statusStr != null) {
                        try {
                            return Integer.parseInt(statusStr) >= 400;
                        } catch(Exception ex) {}
                    }
                    return false;
                })
                .mapToDouble(io.micrometer.core.instrument.Counter::count).sum();
        } catch (Exception e) { return 0; }
    }

    private double getMicrometerLatency(Long userId, String path) {
        try {
            var search = registry.find("gateway.latency").tag("user", userId.toString());
            if (path != null && !path.equals("all")) search = search.tag("path", path);
            Collection<io.micrometer.core.instrument.Timer> timers = search.timers();
            if (timers.isEmpty()) return 0;
            double totalLatency = 0;
            double totalCount = 0;
            for (io.micrometer.core.instrument.Timer t : timers) {
                totalLatency += t.totalTime(TimeUnit.MILLISECONDS);
                totalCount += t.count();
            }
            return totalCount > 0 ? (totalLatency / totalCount) : 0;
        } catch (Exception e) { return 0; }
    }

    private static class ChartPoint {
        long allowed = 0;
        long blocked = 0;
        long totalLatency = 0;
    }
}