package com.major.gateway.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalyticsEvent {
    private Long userId;
    private String path;
    private String targetUrl;
    private String method;
    private int status;
    private long timestamp;
}
