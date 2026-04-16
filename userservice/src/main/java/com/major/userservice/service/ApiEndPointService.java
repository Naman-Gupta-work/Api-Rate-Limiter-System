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
                .orElseThrow(() -> new ApiException("API not found"));
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
    public ApiEndPoint update(Long endpointId,Long userId,ApiEndPoint updatedData){
        ApiEndPoint api = repo.findById(endpointId)
                .orElseThrow(() -> new ApiException("API not found"));

        if (!api.getUser().getId().equals(userId)) {
            throw new ApiException("Unauthorized: You do not own this API endpoint");
        }

        // Enforce path uniqueness during edits
        if (!api.getPath().equals(updatedData.getPath())) {
            if (repo.findByUserIdAndPath(userId, updatedData.getPath()).isPresent()) {
                throw new ApiException("Path already in use by another of your APIs");
            }
        }

        api.setPath(updatedData.getPath());
        api.setTargetUrl(updatedData.getTargetUrl());

        return repo.save(api);
    }

    //  Delete
    public void delete(Long endpointId,Long userId) {
        ApiEndPoint api = repo.findById(endpointId)
                .orElseThrow(() -> new ApiException("API not found"));

        if (!api.getUser().getId().equals(userId)) {
            throw new ApiException("Unauthorized: You do not own this API endpoint");
        }
        repo.delete(api);
    }
}