package com.major.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "user-service", url = "http://localhost:8080")
public interface UserServiceClient {

    @GetMapping("/auth/apikey/validate")
    Map<String, Object> validateApiKey(@RequestParam String key);
}
