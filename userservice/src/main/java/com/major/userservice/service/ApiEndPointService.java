package com.major.userservice.service;

import com.major.userservice.exception.ApiException;
import com.major.userservice.model.ApiEndPoint;
import com.major.userservice.repository.ApiEndPointRepository;
import org.springframework.stereotype.Service;

@Service
public class ApiEndPointService {

    private final ApiEndPointRepository repo;

    public ApiEndPointService(ApiEndPointRepository repo) {
        this.repo = repo;
    }

    public ApiEndPoint getByUserAndPath(Long userId, String path) {
        return repo.findByUserIdAndPath(userId, path)
                .orElseThrow(() -> new RuntimeException("API not found"));
    }
    public ApiEndPoint save(ApiEndPoint apiEndPoint){
        if(repo.findByUserIdAndPath(apiEndPoint.getUser().getId(), apiEndPoint.getPath()).isPresent()){
            throw new ApiException("Path already in use");
        }
        return repo.save(apiEndPoint);
    }
}