package com.major.analytics_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "url_metrics_snapshots")
@Getter
@Setter
public class UrlMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String targetUrl;
    private LocalDateTime timestamp;
    private int requestCount;
    private double averageLatencyMs;
    private int successCount;
    private int errorCount;
    private long totalBandwidthBytes;
}
