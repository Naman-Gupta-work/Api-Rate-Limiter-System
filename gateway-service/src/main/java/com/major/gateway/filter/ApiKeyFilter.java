package com.major.gateway.filter;

import com.major.gateway.client.UserClient;
import com.major.gateway.dto.ApiKeyValidationResponse;
import com.major.gateway.service.AnalyticsService;
import com.major.gateway.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final UserClient userServiceClient;
    private final RateLimitService rateLimitService;
    private final AnalyticsService analyticsService;

    public ApiKeyFilter(UserClient userClient, RateLimitService rateLimitService, AnalyticsService analyticsService) {
        this.userServiceClient = userClient;
        this.rateLimitService = rateLimitService;
        this.analyticsService = analyticsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 1. Skip non-proxy routes
        if (!uri.startsWith("/proxy")) {
            filterChain.doFilter(request, response);
            return;
        }

        String[] parts = uri.split("/", 4);
        String analyticsPath = parts.length >= 4 ? "/" + parts[3] : "/";
        String apiKey = request.getHeader("X-API-KEY");

        // 2. Missing Key check
        if (apiKey == null) {
            analyticsService.recordRequest(0L, analyticsPath, 401, 0);
            sendError(response, HttpStatus.UNAUTHORIZED, "Missing API Key.");
            return;
        }

        try {
            // 3. BLOCKING CALL (This is now perfectly fine with Virtual Threads!)
            ApiKeyValidationResponse validation = userServiceClient.validateApiKey(apiKey);

            if (!validation.isValid()) {
                analyticsService.recordRequest(0L, analyticsPath, 401, 0);
                sendError(response, HttpStatus.UNAUTHORIZED, "Invalid API Key");
                return;
            }

            Long userId = validation.getUserId();

            // 4. Namespace Validation
            if (parts.length >= 3) {
                try {
                    Long urlUserId = Long.parseLong(parts[2]);
                    if (!urlUserId.equals(userId)) {
                        analyticsService.recordRequest(userId, analyticsPath, 403, 0);
                        sendError(response, HttpStatus.FORBIDDEN, "Namespace Access Denied.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    analyticsService.recordRequest(userId, analyticsPath, 400, 0);
                    sendError(response, HttpStatus.BAD_REQUEST, "Invalid User ID format.");
                    return;
                }
            }

            // 5. Rate Limiting
            if (!rateLimitService.isAllowed(apiKey, validation.getPlan())) {
                analyticsService.recordRequest(userId, analyticsPath, 429, 0);
                sendError(response, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded.");
                return;
            }

            // SUCCESS: Attach ID to request and continue
            request.setAttribute("validatedUserId", userId);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            analyticsService.recordRequest(0L, analyticsPath, 500, 0);
            sendError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Gateway Failure");
        }
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"message\": \"" + message + "\"}");
    }
}