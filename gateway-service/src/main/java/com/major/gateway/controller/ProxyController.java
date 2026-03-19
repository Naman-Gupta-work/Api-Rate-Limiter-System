package com.major.gateway.controller;

import com.major.gateway.client.ApiEndpointClient;
import com.major.gateway.client.UserClient;
import com.major.gateway.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;

@RestController
@RequestMapping("/proxy")
public class ProxyController {

    private final WebClient webClient;
    private final ApiEndpointClient apiEndpointClient;
    private final UserClient userClient;

    public ProxyController(WebClient webClient, ApiEndpointClient apiEndpointClient, UserClient userClient) {
        this.webClient = webClient;
        this.apiEndpointClient = apiEndpointClient;
        this.userClient = userClient;
    }

    @RequestMapping("/**")
    public Mono<ResponseEntity<byte[]>> forward(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body
    ) {
        String apiKey = request.getHeader("X-API-KEY");
        UserDto user = userClient.getUserByApiKey(apiKey);
        String path = request.getRequestURI().replace("/proxy", "");
        String targetUrl = apiEndpointClient.resolve(user.getId(), path);
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        String query = request.getQueryString();
        String finalUrl = (query == null || query.isEmpty())
                ? targetUrl
                : targetUrl + "?" + query;

        WebClient.RequestBodySpec spec = webClient
                .method(method)
                .uri(finalUrl);
        spec.header("User-Agent", "Mozilla/5.0");
        for (String headerName : Collections.list(request.getHeaderNames())) {
            if (headerName.equalsIgnoreCase("host") ||
                    headerName.equalsIgnoreCase("connection") ||
                    headerName.equalsIgnoreCase("content-length") ||
                    headerName.equalsIgnoreCase("x-api-key")) {
                continue;
            }
            spec.header(headerName, request.getHeader(headerName));
        }

        WebClient.ResponseSpec responseSpec;

        if (body != null && body.length > 0) {
            responseSpec = spec.bodyValue(body).retrieve();
        } else {
            responseSpec = spec.retrieve();
        }

        return responseSpec.toEntity(byte[].class);
    }
}