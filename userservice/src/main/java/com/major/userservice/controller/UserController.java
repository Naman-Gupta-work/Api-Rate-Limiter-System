package com.major.userservice.controller;

import com.major.userservice.dto.UserDto;
import com.major.userservice.model.User;
import com.major.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/by-api-key")
    @Operation(summary = " returns User's ID for Api Key")
    public UserDto getUserByApiKey(@RequestParam String apiKey) {
        User user = service.getByApiKey(apiKey);
        return new UserDto(user.getId());
    }


}
