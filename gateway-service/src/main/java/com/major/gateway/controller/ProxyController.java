package com.major.gateway.controller;

import com.major.gateway.client.ApiEndpointClient;
import com.major.gateway.client.UserClient;
import com.major.gateway.dto.AnalyticsEvent;
import com.major.gateway.dto.UserDto;
import com.major.gateway.service.KafkaProducerService;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/proxy")
public class ProxyController {

    private final WebClient webClient;
    private final ApiEndpointClient apiEndpointClient;
    private final UserClient userClient;
    private final KafkaProducerService kafkaProducer;

    public ProxyController(WebClient webClient, ApiEndpointClient apiEndpointClient, UserClient userClient, KafkaProducerService kafkaProducer) {
        this.webClient = webClient;
        this.apiEndpointClient = apiEndpointClient;
        this.userClient = userClient;
        this.kafkaProducer = kafkaProducer;
    }

    @RequestMapping("/**")
    public Mono<ResponseEntity<byte[]>> forward(
            ServerHttpRequest request,
            @RequestBody(required = false) byte[] body
    ) {

        String apiKey = request.getHeaders().getFirst("X-API-KEY");
        UserDto user = userClient.getUserByApiKey(apiKey);


        String path = request.getPath().value().replace("/proxy", "");
        String targetUrl = apiEndpointClient.resolve(user.getId(), path);

        HttpMethod method = request.getMethod();

        String query = request.getURI().getQuery();
        String finalUrl = (query == null || query.isEmpty())
                ? targetUrl
                : targetUrl + "?" + query;

        WebClient.RequestBodySpec spec = webClient
                .method(method)
                .uri(finalUrl);

        spec.header("User-Agent", "Mozilla/5.0");

        request.getHeaders().forEach((headerName, headerValues) -> {
            if (headerName.equalsIgnoreCase("host") ||
                    headerName.equalsIgnoreCase("connection") ||
                    headerName.equalsIgnoreCase("content-length") ||
                    headerName.equalsIgnoreCase("x-api-key")) {
                return; // skip
            }
            headerValues.forEach(value -> spec.header(headerName, value));
        });

        WebClient.ResponseSpec responseSpec = (body != null && body.length > 0)
                ? spec.bodyValue(body).retrieve()
                : spec.retrieve();

        return responseSpec.toEntity(byte[].class).map(response -> {
            AnalyticsEvent analyticsEvent = new AnalyticsEvent();
            analyticsEvent.setUserId(user.getId());
            analyticsEvent.setPath(path);
            analyticsEvent.setTargetUrl(targetUrl);
            analyticsEvent.setMethod(method.name());
            analyticsEvent.setStatus(response.getStatusCode().value());
            analyticsEvent.setTimestamp(System.currentTimeMillis());

            kafkaProducer.sendAnalyticsEvent(analyticsEvent);

            return response;
        });
    }
}