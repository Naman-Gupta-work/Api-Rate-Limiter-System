package com.major.userservice.service;

import com.major.userservice.model.ApiKey;
import com.major.userservice.model.User;
import com.major.userservice.repository.ApiKeyRepository;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository){
        this.apiKeyRepository=apiKeyRepository;

    }

    public ApiKey generateApiKey(User user){
        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        return apiKeyRepository.save(apiKey);
    }

    public ApiKey validateKey(String key) {

        return apiKeyRepository.findByApiKey(key)
                .filter(ApiKey::isActive)
                .orElseThrow(() -> new RuntimeException("Invalid API Key"));
    }
}
