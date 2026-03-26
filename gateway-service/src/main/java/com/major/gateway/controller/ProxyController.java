package com.major.gateway.controller;

import com.major.gateway.client.ApiEndpointClient;
import com.major.gateway.dto.AnalyticsEvent;
import com.major.gateway.dto.ApiResponse;
import com.major.gateway.service.KafkaProducerService;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest; // <-- Correct Reactive Import!
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/proxy")
public class ProxyController {

    private final WebClient webClient;
    private final ApiEndpointClient apiEndpointClient;
    private final KafkaProducerService kafkaProducer;

    public ProxyController(WebClient webClient, ApiEndpointClient apiEndpointClient, KafkaProducerService kafkaProducer) {
        this.webClient = webClient;
        this.apiEndpointClient = apiEndpointClient;
        this.kafkaProducer = kafkaProducer;
    }

    @RequestMapping("/**")
    public Mono<ResponseEntity<byte[]>> forward(
            ServerHttpRequest request,
            @RequestAttribute(value = "validatedUserId", required = false) Long userId, // <-- Magic extraction from the Filter!
            @RequestBody(required = false) byte[] body
    ) {
        long startTime = System.currentTimeMillis();

        // 1. Reactive IP Extraction
        final String clientIp = request.getRemoteAddress().getAddress().getHostAddress();


        // 2. Validate User Context
        if (userId == null) {
            return Mono.just(ResponseEntity.status(401).body("{\"message\": \"Unauthorized: Missing User Context\"}".getBytes()));
        }

        // 3. Reactive Path Extraction
        String uri = request.getURI().getPath();
        String prefixToStrip = "/proxy/" + userId;

        String tempPath = "";
        if (uri.startsWith(prefixToStrip)) {
            tempPath = uri.substring(prefixToStrip.length());
        }
        final String path = tempPath.isEmpty() ? "/" : tempPath;

        // 4. Resolve the Target URL
        String targetUrl;
        try {
            ApiResponse resolveResponse = apiEndpointClient.resolve(userId, path);
            targetUrl = resolveResponse.getMessage();
        } catch (Exception e) {
            return Mono.just(ResponseEntity.status(404).body("{\"message\": \"API Endpoint not found for this user\"}".getBytes()));
        }

        // 5. Construct Final URL (Reactive Query String)
        String query = request.getURI().getQuery();
        String finalUrl = (query == null || query.isEmpty()) ? targetUrl : targetUrl + "?" + query;

        // 6. Map HTTP Method and Headers
        HttpMethod method = request.getMethod(); // In WebFlux, this already returns an HttpMethod object
        WebClient.RequestBodySpec spec = webClient.method(method).uri(finalUrl);

        // Reactive Header Loop (No more Enumeration!)
        request.getHeaders().forEach((headerName, headerValues) -> {
            if (!isExcludedHeader(headerName)) {
                spec.header(headerName, headerValues.toArray(new String[0]));
            }
        });
        spec.header("User-Agent", "API-Gateway");

        // 7. Execute Proxy Call and Send Analytics
        WebClient.ResponseSpec responseSpec = (body != null && body.length > 0)
                ? spec.bodyValue(body).retrieve()
                : spec.retrieve();

        return responseSpec.toEntity(byte[].class)
                .map(response -> {
                    sendAnalytics(userId, path, targetUrl, method.name(), response.getStatusCode().value(), startTime, clientIp, response.getBody());
                    return response;
                })
                .onErrorResume(error -> {
                    // Catch connection timeouts to the downstream target API
                    sendAnalytics(userId, path, targetUrl, method.name(), 502, startTime, clientIp, null);
                    return Mono.just(ResponseEntity.status(502).body("{\"message\": \"Bad Gateway: Target API unreachable\"}".getBytes()));
                });
    }

    private boolean isExcludedHeader(String headerName) {
        return headerName.equalsIgnoreCase("host") ||
                headerName.equalsIgnoreCase("connection") ||
                headerName.equalsIgnoreCase("content-length") ||
                headerName.equalsIgnoreCase("x-api-key");
    }

    private void sendAnalytics(Long userId, String path, String targetUrl, String method, int status, long startTime, String clientIp, byte[] body) {
        AnalyticsEvent event = new AnalyticsEvent();
        event.setUserId(userId);
        event.setPath(path);
        event.setTargetUrl(targetUrl);
        event.setMethod(method);
        event.setStatus(status);
        event.setTimestamp(System.currentTimeMillis());
        event.setLatencyMs(System.currentTimeMillis() - startTime);
        event.setClientIp(clientIp);
        event.setResponseSizeBytes(body != null ? body.length : 0);

        kafkaProducer.sendAnalyticsEvent(event);
    }
}