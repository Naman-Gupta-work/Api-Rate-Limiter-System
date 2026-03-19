package com.major.userservice.service;

import com.major.userservice.model.ApiKey;
import com.major.userservice.model.Plan;
import com.major.userservice.model.User;
import com.major.userservice.repository.ApiKeyRepository;
import com.major.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ApiKeyRepository apiKeyRepository;


    public User register(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        user.setPlan(Plan.FREE);
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.
                findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getByApiKey(String apiKey) {

        ApiKey key = apiKeyRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Key not found"));

        return key.getUser();
    }


}
