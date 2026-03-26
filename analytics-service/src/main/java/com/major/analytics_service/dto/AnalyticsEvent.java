package com.major.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AnalyticsEvent {
    private Long userId;
    private String path;
    private String targetUrl;
    private String method;
    private int status;
    private long timestamp;
    private long latencyMs;
    private String clientIp;
    private long responseSizeBytes;
}
