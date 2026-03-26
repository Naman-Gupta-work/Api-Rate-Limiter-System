package com.major.gateway.controller;

import com.major.gateway.client.ApiEndpointClient;
import com.major.gateway.dto.AnalyticsEvent;
import com.major.gateway.dto.ApiResponse;
import com.major.gateway.service.KafkaProducerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Enumeration;

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
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body
    ) {
        long startTime = System.currentTimeMillis();
        String clientIp = request.getRemoteAddr();

        // 1. Get the User ID from the filter
        Long userId = (Long) request.getAttribute("validatedUserId");

        // 2. Resolve the Target URL
        String path = request.getRequestURI().replace("/proxy", "");

        String targetUrl;
        try {
            ApiResponse resolveResponse = apiEndpointClient.resolve(userId, path);
            targetUrl = resolveResponse.getMessage(); // Extracting URL from our new JSON structure
        } catch (Exception e) {
            return Mono.just(ResponseEntity.status(404).body("{\"message\": \"API Endpoint not found for this user\"}".getBytes()));
        }

        // 3. Construct the Final Request
        String query = request.getQueryString();
        String finalUrl = (query == null || query.isEmpty()) ? targetUrl : targetUrl + "?" + query;

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        WebClient.RequestBodySpec spec = webClient.method(method).uri(finalUrl);

        // Map Headers
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!isExcludedHeader(headerName)) {
                spec.header(headerName, request.getHeader(headerName));
            }
        }
        spec.header("User-Agent", "API-Gateway");

        // 4. Execute Proxy Call and Send Analytics
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