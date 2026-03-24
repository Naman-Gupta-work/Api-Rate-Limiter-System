package com.major.gateway.client;

import com.major.gateway.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "user-client", url = "http://localhost:8082")
public interface UserClient {

    @GetMapping("/auth/apikey/validate")
    Map<String, Object> validateApiKey(@RequestParam String key);

    @GetMapping("/users/by-api-key")
    UserDto getUserByApiKey(@RequestParam String apiKey);
}
