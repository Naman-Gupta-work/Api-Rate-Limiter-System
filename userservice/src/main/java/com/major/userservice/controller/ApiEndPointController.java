package com.major.userservice.controller;

import com.major.userservice.model.ApiEndPoint;
import com.major.userservice.service.ApiEndPointService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api-endpoints")
public class ApiEndPointController {

    private final ApiEndPointService service;

    public ApiEndPointController(ApiEndPointService service) {
        this.service = service;
    }

    @PostMapping
    public ApiEndPoint create(@RequestBody ApiEndPoint request) {
        return service.save(request);
    }

    @GetMapping("/resolve")
    public String resolve(
            @RequestParam Long userId,
            @RequestParam String path
    ) {
        return service.getByUserAndPath(userId, path).getTargetUrl();
    }
}
