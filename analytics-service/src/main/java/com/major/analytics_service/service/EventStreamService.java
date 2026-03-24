package com.major.analytics_service.service;

import com.major.analytics_service.dto.AnalyticsEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class EventStreamService {
    private final List<AnalyticsEvent> events = new CopyOnWriteArrayList<>();

    public List<AnalyticsEvent> getAllEvents() {
        return events;
    }

    private final Sinks.Many<AnalyticsEvent> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    //  Called when new analytics event comes
    public void publish(AnalyticsEvent event) {
        events.add(event);
        sink.tryEmitNext(event);
    }

    // Controller will expose this to frontend
    public Flux<AnalyticsEvent> getStream() {
        return sink.asFlux();
    }
}
