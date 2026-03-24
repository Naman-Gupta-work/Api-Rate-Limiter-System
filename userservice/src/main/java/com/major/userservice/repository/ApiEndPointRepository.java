package com.major.userservice.repository;

import com.major.userservice.model.ApiEndPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiEndPointRepository extends JpaRepository<ApiEndPoint,Long> {

    Optional<ApiEndPoint>  findByUserIdAndPath(Long userId, String path);
    List<ApiEndPoint> findAllByUserId(Long userId);
}
