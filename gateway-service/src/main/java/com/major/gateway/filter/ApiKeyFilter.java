package com.major.gateway.filter;

import com.major.gateway.client.UserClient;
import com.major.gateway.dto.ApiKeyValidationResponse;
import com.major.gateway.service.RateLimitService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiKeyFilter implements Filter {

    private final UserClient userServiceClient;
    private final RateLimitService rateLimitService;

    public ApiKeyFilter(UserClient userClient, RateLimitService rateLimitService) {
        this.userServiceClient = userClient;
        this.rateLimitService = rateLimitService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Skip filter for non-proxy routes (if you have any swagger docs etc.)
        if (!req.getRequestURI().startsWith("/proxy")) {
            chain.doFilter(request, response);
            return;
        }

        String apiKey = req.getHeader("X-API-KEY");

        if (apiKey == null) {
            sendError(res, HttpServletResponse.SC_UNAUTHORIZED, "Missing API Key. Include 'X-API-KEY' header.");
            return;
        }

        try {
            // Validate via User Service
            ApiKeyValidationResponse validation = userServiceClient.validateApiKey(apiKey);

            if (!validation.isValid()) {
                sendError(res, HttpServletResponse.SC_UNAUTHORIZED, "Invalid API Key");
                return;
            }

            // Check Rate Limit
            if (!rateLimitService.isAllowed(apiKey, validation.getPlan())) {
                sendError(res, 429, "Rate limit exceeded for plan: " + validation.getPlan());
                return;
            }

            // HUGE PERFORMANCE FIX: Attach the userId to the request so the Controller doesn't have to fetch it again!
            req.setAttribute("validatedUserId", validation.getUserId());

            chain.doFilter(request, response);

        } catch (Exception e) {
            sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Gateway Auth Failure");
        }
    }

    // Helper method to ensure the frontend/clients always get JSON
    private void sendError(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().write("{\"message\": \"" + message + "\"}");
    }
}