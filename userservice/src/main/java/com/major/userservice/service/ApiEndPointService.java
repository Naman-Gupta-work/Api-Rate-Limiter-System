package com.major.userservice.service;

import com.major.userservice.exception.ApiException;
import com.major.userservice.model.ApiEndPoint;
import com.major.userservice.repository.ApiEndPointRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
    // Create
    public ApiEndPoint create(ApiEndPoint apiEndPoint){
        if(repo.findByUserIdAndPath(apiEndPoint.getUser().getId(), apiEndPoint.getPath()).isPresent()){
            throw new ApiException("Path already in use");
        }
        return repo.save(apiEndPoint);
    }
    // Get all
    public List<ApiEndPoint> getAll(Long userId){
        return repo.findAllByUserId(userId);
    }

    // Update
    public ApiEndPoint update(Long userId,ApiEndPoint apiEndPoint){
        ApiEndPoint  api = repo.findById(userId).orElseThrow(() -> new ApiException("API not found"));

        api.setPath(apiEndPoint.getPath());
        api.setTargetUrl(apiEndPoint.getTargetUrl());

        return repo.save(api);
    }

    //  Delete
    public void delete(Long id) {
        repo.deleteById(id);
    }
}