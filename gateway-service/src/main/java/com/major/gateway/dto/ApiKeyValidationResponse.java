package com.major.gateway.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiKeyValidationResponse {
    private boolean valid;
    private Long userId;
    private String plan;
}