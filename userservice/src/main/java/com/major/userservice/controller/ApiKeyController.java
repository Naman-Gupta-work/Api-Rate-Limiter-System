package com.major.userservice.controller;

import com.major.userservice.dto.ApiKeyValidationResponse;
import com.major.userservice.dto.ApiResponse;
import com.major.userservice.model.User;
import com.major.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/apikey")
public class ApiKeyController {

    private final UserService userService;

    public ApiKeyController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generates a new Api Key for a user")
    public ResponseEntity<ApiResponse> generateApiKey(@RequestParam String email) {
        User user = userService.findByEmail(email);
        String newKey = userService.generateNewApiKey(user);
        return ResponseEntity.ok(new ApiResponse(newKey));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validates Api key and returns associated User's Id and plan")
    public ResponseEntity<ApiKeyValidationResponse> validateApiKey(@RequestParam String key) {
        try {
            User user = userService.getByApiKey(key);
            return ResponseEntity.ok(new ApiKeyValidationResponse(true, user.getId(), user.getPlan().name()));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiKeyValidationResponse(false, null, null));
        }
    }
}