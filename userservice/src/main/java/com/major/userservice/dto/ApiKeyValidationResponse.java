package com.major.userservice.dto;

import lombok.Getter;

@Getter
public class ApiKeyValidationResponse {

    private boolean valid;
    private Long userId;
    private String plan;

    public ApiKeyValidationResponse(boolean valid, Long userId,String plan) {
        this.valid = valid;
        this.userId = userId;
        this.plan = plan;
    }


}
