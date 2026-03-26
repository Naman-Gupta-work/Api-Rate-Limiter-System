package com.major.analytics_service.repository;



import com.major.analytics_service.entity.UrlMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlMetricsRepository extends JpaRepository<UrlMetrics, Long> {
}
