package com.major.userservice.controller;

import com.major.userservice.dto.ApiKeyValidationResponse;
import com.major.userservice.dto.LoginRequest;
import com.major.userservice.dto.RegisterRequest;
import com.major.userservice.model.ApiKey;
import com.major.userservice.model.User;
import com.major.userservice.security.JwtUtil;
import com.major.userservice.service.ApiKeyService;
import com.major.userservice.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final ApiKeyService apiKeyService;

    public AuthController(UserService userService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder, ApiKeyService apiKeyService) {
        this.userService = userService;
        this.jwtUtil=jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.apiKeyService = apiKeyService;
    }

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) {
        if(userService.findByEmail(request.getEmail())!=null){
            return "User Already exists";
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());

        userService.register(user);

        return "User registered successfully";
    }
    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {

        User user = userService.findByEmail(request.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return jwtUtil.generateToken(user.getEmail());
    }

    @PostMapping("/apikey")
    public String generateApiKey(@RequestParam String email) {

        User user = userService.findByEmail(email);

        return apiKeyService.generateApiKey(user).getApiKey();
    }

    @GetMapping("/apikey/validate")
    public ApiKeyValidationResponse validateApiKey(@RequestParam String key) {
        try {
            ApiKey apiKey = apiKeyService.validateKey(key);
            User user=apiKey.getUser();

            return new ApiKeyValidationResponse(true, user.getId(),user.getPlan().name());

        } catch (Exception e) {
            return new ApiKeyValidationResponse(false, null,null);
        }
    }
}
