package com.major.gateway.client;

import com.major.gateway.dto.ApiKeyValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-client", url = "http://localhost:8080")
public interface UserClient {
    // We now map directly to the DTO instead of a messy Map<String, Object>
    @GetMapping("/apikey/validate")
    ApiKeyValidationResponse validateApiKey(@RequestParam("key") String key);
}