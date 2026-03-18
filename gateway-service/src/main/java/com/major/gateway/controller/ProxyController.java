package com.major.gateway.controller;

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

    public ProxyController(WebClient webClient) {
        this.webClient = webClient;
    }

    @RequestMapping("/**")
    public Mono<ResponseEntity<byte[]>> forward(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body
    ) {


        String path = request.getRequestURI().replace("/proxy", "");


        String targetUrl = "https://postman-echo.com" + path;

        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        WebClient.RequestBodySpec spec = webClient
                .method(method)
                .uri(targetUrl);
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