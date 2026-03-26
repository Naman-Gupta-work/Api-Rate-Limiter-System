package com.major.analytics_service.service;

import com.major.analytics_service.dto.AnalyticsEvent;
import com.major.analytics_service.entity.UrlMetrics;
import com.major.analytics_service.repository.UrlMetricsRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AnalyticsConsumer {

    private final EventStreamService streamService;
    private final UrlMetricsRepository urlMetricsRepository;

    public AnalyticsConsumer(EventStreamService streamService, UrlMetricsRepository urlMetricsRepository) {
        this.streamService = streamService;
        this.urlMetricsRepository = urlMetricsRepository;
    }


    @KafkaListener(topics = "analytics-topic", groupId = "analytics-group")
    public void consumeBatch(List<AnalyticsEvent> events) {
        if (events.isEmpty()) return;

        Map<String, List<AnalyticsEvent>> groupedEvents = events.stream()
                .collect(Collectors.groupingBy(AnalyticsEvent::getTargetUrl));
        for (Map.Entry<String, List<AnalyticsEvent>> entry : groupedEvents.entrySet()) {
            String targetUrl = entry.getKey();
            List<AnalyticsEvent> urlEvents = entry.getValue();
            int totalRequests = urlEvents.size();
            double avgLatency = urlEvents.stream()
                    .mapToLong(AnalyticsEvent::getLatencyMs)
                    .average()
                    .orElse(0.0);
            int successCount = (int) urlEvents.stream()
                    .filter(e -> e.getStatus() >= 200 && e.getStatus() < 400)
                    .count();

            long totalBandwidth = urlEvents.stream()
                    .mapToLong(AnalyticsEvent::getResponseSizeBytes)
                    .sum();

            UrlMetrics urlMetrics = new UrlMetrics();
            urlMetrics.setTargetUrl(targetUrl);
            urlMetrics.setTimestamp(LocalDateTime.now());
            urlMetrics.setRequestCount(totalRequests);
            urlMetrics.setAverageLatencyMs(avgLatency);
            urlMetrics.setSuccessCount(successCount);
            urlMetrics.setErrorCount(totalRequests-successCount);
            urlMetrics.setTotalBandwidthBytes(totalBandwidth);

            urlMetricsRepository.save(urlMetrics);
        }
        events.forEach(streamService::publish);
    }
}