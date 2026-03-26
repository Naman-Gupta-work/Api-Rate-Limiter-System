package com.major.gateway.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.major.gateway.dto.ApiResponse;


@FeignClient(name = "api-endpoint-client", url = "http://localhost:8080")
public interface ApiEndpointClient {
    // Returns the new ApiResponse containing the target URL in the "message" field
    @GetMapping("/api-endpoints/resolve")
    ApiResponse resolve(@RequestParam("userId") Long userId, @RequestParam("path") String path);
}
