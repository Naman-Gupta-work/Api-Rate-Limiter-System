package com.major.userservice.controller;

import com.major.userservice.model.ApiEndPoint;
import com.major.userservice.service.ApiEndPointService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api-endpoints")
public class ApiEndPointController {

    private final ApiEndPointService service;

    public ApiEndPointController(ApiEndPointService service) {
        this.service = service;
    }

    @PostMapping
    public ApiEndPoint create(@RequestBody ApiEndPoint request) {
                return service.create(request);
    }

    @GetMapping("/{userId}")
    public List<ApiEndPoint> findAllByUserId(@PathVariable Long userId) {
        return service.getAll(userId);
    }

    @PutMapping("/{id}")
    public ApiEndPoint update(@PathVariable Long id, @RequestBody ApiEndPoint api) {
        return service.update(id, api);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Deleted successfully";
    }

    @GetMapping("/resolve")
    public String resolve(@RequestParam("userId") Long userId,@RequestParam("path") String path) {
        return service.getByUserAndPath(userId, path).getTargetUrl();
    }
}
