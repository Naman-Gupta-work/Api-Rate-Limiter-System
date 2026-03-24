package com.major.analytics_service.controller;



import com.major.analytics_service.dto.AnalyticsEvent;
import com.major.analytics_service.service.EventStreamService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final EventStreamService streamService;

    public AnalyticsController(EventStreamService streamService) {
        this.streamService = streamService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AnalyticsEvent> stream(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String targetUrl
    ) {
        return streamService.getStream()
                .filter(event ->
                                (userId == null || event.getUserId().equals(userId)) &&
                                (path == null || event.getPath().equals(path)) &&
                                (targetUrl == null || event.getTargetUrl().equals(targetUrl))
                );
    }
}
