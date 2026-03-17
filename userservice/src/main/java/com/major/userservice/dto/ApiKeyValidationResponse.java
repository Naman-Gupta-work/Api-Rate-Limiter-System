package com.major.userservice.dto;

public class ApiKeyValidationResponse {

    private boolean valid;
    private Long userId;

    public ApiKeyValidationResponse(boolean valid, Long userId) {
        this.valid = valid;
        this.userId = userId;
    }

    public boolean isValid() {
        return valid;
    }

    public Long getUserId() {
        return userId;
    }
}
