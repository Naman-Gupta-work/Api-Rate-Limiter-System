package com.major.gateway.service;

import com.major.gateway.client.ApiEndpointClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.major.gateway.dto.ApiResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.Collections;
import java.util.List;


@Service
public class ProxyService {

    private final RestClient restClient;
    private final ApiEndpointClient apiEndpointClient;
    private final AnalyticsService analyticsService;

    public ProxyService(RestClient.Builder restClientBuilder, ApiEndpointClient apiEndpointClient, AnalyticsService analyticsService) {
        this.restClient = restClientBuilder.build();
        this.apiEndpointClient = apiEndpointClient;
        this.analyticsService = analyticsService;
    }

    public ResponseEntity<byte[]> forwardRequest(HttpServletRequest request, Long userId, byte[] body) {
        long startTime = System.currentTimeMillis();

        if (userId == null) {
            return ResponseEntity.status(401).body("{\"message\": \"Unauthorized: Missing User Context\"}".getBytes());
        }

        // 1. Resolve Target URL (This is now a simple blocking call)
        String uri = request.getRequestURI();
        String prefixToStrip = "/proxy/" + userId;
        String path = uri.startsWith(prefixToStrip) ? uri.substring(prefixToStrip.length()) : "/";
        if (path.isEmpty()) path = "/";

        String targetUrl;
        try {
            ApiResponse resolveResponse = apiEndpointClient.resolve(userId, path);
            targetUrl = resolveResponse.getMessage(); // Assuming this returns the base target URL
        } catch (Exception e) {
            return ResponseEntity.status(404).body("{\"message\": \"API Endpoint not found\"}".getBytes());
        }

        // 2. Prepare Final URL with Query Params
        String query = request.getQueryString();
        String finalUrl = (query == null) ? targetUrl : targetUrl + "?" + query;

        try {
            // 3. Execute Proxy Call (Blocking but running on a Virtual Thread)
            ResponseEntity<byte[]> response = restClient.method(HttpMethod.valueOf(request.getMethod()))
                    .uri(finalUrl)
                    .headers(headers -> {
                        Collections.list(request.getHeaderNames()).forEach(headerName -> {
                            if (!isExcludedHeader(headerName)) {
                                headers.add(headerName, request.getHeader(headerName));
                            }
                        });
                        headers.set("User-Agent", "Obsidian-Gateway");
                    })
                    .body(body != null ? body : new byte[0])
                    .retrieve()
                    .toEntity(byte[].class);

            // 4. Record Analytics
            analyticsService.recordRequest(userId, path, response.getStatusCode().value(), System.currentTimeMillis() - startTime);
            return response;

        } catch (Exception error) {
            // 5. Handle Target API failure (Bad Gateway)
            analyticsService.recordRequest(userId, path, 502, System.currentTimeMillis() - startTime);
            return ResponseEntity.status(502).body("{\"message\": \"Target API unreachable\"}".getBytes());
        }
    }

    private boolean isExcludedHeader(String headerName) {
        return List.of("host", "connection", "content-length", "x-api-key")
                .contains(headerName.toLowerCase());
    }
}
