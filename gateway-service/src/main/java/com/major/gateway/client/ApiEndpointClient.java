package com.major.gateway.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "api-endpoint-client", url = "http://localhost:8082")
public interface ApiEndpointClient {

    @GetMapping("/api-endpoints/resolve")
     String resolve(
            @RequestParam Long userId,
            @RequestParam String path
    );
}
