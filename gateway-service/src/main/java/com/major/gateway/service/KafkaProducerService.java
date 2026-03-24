package com.major.gateway.service;

import com.major.gateway.dto.AnalyticsEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, AnalyticsEvent> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, AnalyticsEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendAnalyticsEvent(AnalyticsEvent analyticsEvent) {
        kafkaTemplate.send("analytics-topic",analyticsEvent);
    }
}
