package com.major.gateway.filter;

import com.major.gateway.client.UserClient;
import com.major.gateway.dto.ApiKeyValidationResponse;
import com.major.gateway.service.RateLimitService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class ApiKeyFilter implements WebFilter {

    private final UserClient userServiceClient;
    private final RateLimitService rateLimitService;

    public ApiKeyFilter(UserClient userClient, RateLimitService rateLimitService) {
        this.userServiceClient = userClient;
        this.rateLimitService = rateLimitService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String uri = request.getURI().getPath();

        // 1. Skip filter for non-proxy routes
        if (!uri.startsWith("/proxy")) {
            return chain.filter(exchange);
        }

        // 2. Check for API Key
        String apiKey = request.getHeaders().getFirst("X-API-KEY");
        if (apiKey == null) {
            return sendError(exchange, HttpStatus.UNAUTHORIZED, "Missing API Key. Include 'X-API-KEY' header.");
        }

        try {
            // 3. Validate via User Service
            ApiKeyValidationResponse validation = userServiceClient.validateApiKey(apiKey);

            if (!validation.isValid()) {
                return sendError(exchange, HttpStatus.UNAUTHORIZED, "Invalid API Key");
            }

            // 4. Validate URL matches Token ID
            String[] uriParts = uri.split("/");
            if (uriParts.length >= 3) {
                try {
                    Long urlUserId = Long.parseLong(uriParts[2]);
                    if (!urlUserId.equals(validation.getUserId())) {
                        return sendError(exchange, HttpStatus.FORBIDDEN, "Access Denied: You do not own this routing namespace.");
                    }
                } catch (NumberFormatException e) {
                    return sendError(exchange, HttpStatus.BAD_REQUEST, "Invalid User ID format in URL.");
                }
            } else {
                return sendError(exchange, HttpStatus.BAD_REQUEST, "Malformed proxy URL.");
            }

            // 5. Check Rate Limit
            if (!rateLimitService.isAllowed(apiKey, validation.getPlan())) {
                return sendError(exchange, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded for plan: " + validation.getPlan());
            }

            // 6. Attach userId to the Reactive Exchange context!
            exchange.getAttributes().put("validatedUserId", validation.getUserId());

            return chain.filter(exchange);

        } catch (Exception e) {
            return sendError(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Gateway Auth Failure");
        }
    }

    // Helper method to send JSON errors reactively
    private Mono<Void> sendError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String json = "{\"message\": \"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}