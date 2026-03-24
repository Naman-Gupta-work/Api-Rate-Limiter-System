package com.major.analytics_service.service;

import com.major.analytics_service.dto.AnalyticsEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class AnalyticsConsumer {

    private final EventStreamService streamService;

    public AnalyticsConsumer(EventStreamService streamService) {
        this.streamService = streamService;
    }


    @KafkaListener(topics = "analytics-topic", groupId = "analytics-group")
    public void consume(AnalyticsEvent event) {
        streamService.publish(event);
    }
}
