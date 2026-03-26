package com.major.userservice.service;

import com.major.userservice.exception.ApiException;
import com.major.userservice.model.User;
import com.major.userservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepository, EmailService emailService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public User register(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new ApiException("Email already registered"); // Changed to ApiException
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user); // Defaults are handled by @PrePersist in the Entity!
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found"));
    }

    public User getByApiKey(String apiKey) {
        return userRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new ApiException("Invalid API Key"));
    }

    public String generateNewApiKey(User user) {
        user.setApiKey(UUID.randomUUID().toString());
        userRepository.save(user);
        return user.getApiKey();
    }

    public void generateAndSendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("If that email is registered, an OTP has been sent."));

        // Generate a 6-digit random OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        user.setResetOtp(otp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendOtpEmail(user.getEmail(), otp);
    }

    public void verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Invalid email or OTP"));

        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp)) {
            throw new ApiException("Invalid OTP");
        }

        if (user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new ApiException("OTP has expired. Please request a new one.");
        }
    }

    public void resetPassword(String email, String otp, String newPassword) {
        // Re-verify the OTP just in case someone hits this endpoint directly
        verifyOtp(email, otp);

        User user = findByEmail(email);
        user.setPassword(passwordEncoder.encode(newPassword));

        user.setResetOtp(null);
        user.setResetOtpExpiry(null);

        userRepository.save(user);
    }
}