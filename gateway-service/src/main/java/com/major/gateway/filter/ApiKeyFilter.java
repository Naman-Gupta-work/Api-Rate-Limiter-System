package com.major.gateway.filter;

import com.major.gateway.client.UserServiceClient;
import com.major.gateway.service.RateLimitService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class ApiKeyFilter implements Filter {

    private final UserServiceClient userServiceClient;
    private final RateLimitService  rateLimitService;

    public ApiKeyFilter(UserServiceClient userServiceClient, RateLimitService rateLimitService) {
        this.userServiceClient = userServiceClient;
        this.rateLimitService = rateLimitService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String apiKey = req.getHeader("X-API-KEY");

        // Missing key
        if (apiKey == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().write("Missing API Key");
            return;
        }

        // Validate via User Service
        Map<String, Object> result = userServiceClient.validateApiKey(apiKey);

        Boolean valid = (Boolean) result.get("valid");

        // Invalid key
        if (valid == null || !valid) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().write("Invalid API Key");
            return;
        }
        String plan = (String)result.get("plan");

        if(!rateLimitService.isAllowed(apiKey,plan)){
            res.setStatus(429);
            res.getWriter().write("Too many requests");
            return;
        }

        // Allow request
        chain.doFilter(request, response);
    }
}