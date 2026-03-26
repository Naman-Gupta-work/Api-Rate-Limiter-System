package com.major.userservice.controller;

import com.major.userservice.dto.*;
import com.major.userservice.exception.ApiException;
import com.major.userservice.model.User;
import com.major.userservice.security.JwtUtil;
import com.major.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = " Auth Controller", description = "Handles Registration and Login")
public class AuthController {
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    public AuthController(UserService userService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    @Operation(summary = "Registers a new user")
    public ResponseEntity<ApiResponse> signup(@RequestBody RegisterRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        userService.register(user);

        // Returns { "message": "User registered successfully" }
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("User registered successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "Login a pre-existing User")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        User user = userService.findByEmail(request.getEmail());

        if (!passwordEncoder.matches(request.getPassword(),user.getPassword())) {
            throw new ApiException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getApiKey()));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Sends an OTP to the user's email")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        userService.generateAndSendOtp(request.getEmail());
        return ResponseEntity.ok(new ApiResponse("OTP sent successfully to your email."));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verifies the OTP sent to the email")
    public ResponseEntity<ApiResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        userService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(new ApiResponse("OTP verified successfully."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Resets the user's password using the OTP")
    public ResponseEntity<ApiResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(new ApiResponse("Password reset successfully. You can now log in."));
    }
}