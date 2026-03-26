package com.major.userservice.controller;

import com.major.userservice.dto.ApiResponse;
import com.major.userservice.model.ApiEndPoint;
import com.major.userservice.model.User;
import com.major.userservice.service.ApiEndPointService;
import com.major.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "API End Point Management", description = "CRUD operations and Resolving")
@RequestMapping("/api-endpoints")
public class ApiEndPointController {

    private final ApiEndPointService service;
    private final UserService userService;

    public ApiEndPointController(ApiEndPointService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        return userService.findByEmail(email);
    }

    @PostMapping
    @Operation(summary = "Creates a new api end point")
    public ApiEndPoint create(@RequestBody ApiEndPoint request, Authentication authentication) {
        User loggedInUser = getAuthenticatedUser(authentication);
        request.setUser(loggedInUser);
        return service.create(request);
    }

    @GetMapping
    @Operation(summary = "Lists all end points of the logged-in user")
    public List<ApiEndPoint> findAllForUser(Authentication authentication) {
        User loggedInUser = getAuthenticatedUser(authentication);
        return service.getAll(loggedInUser.getId());
    }

    @PutMapping("/{endpointId}")
    @Operation(summary = "Updates pre-existing api end point")
    public ApiEndPoint update(@PathVariable Long endpointId, @RequestBody ApiEndPoint api, Authentication authentication) {
        User loggedInUser = getAuthenticatedUser(authentication);
        return service.update(endpointId, loggedInUser.getId(), api);
    }

    @DeleteMapping("/{endpointId}")
    @Operation(summary = "Deletes an api end point")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long endpointId, Authentication authentication) {
        User loggedInUser = getAuthenticatedUser(authentication);
        service.delete(endpointId, loggedInUser.getId());
        return ResponseEntity.ok(new ApiResponse("Deleted successfully"));
    }

    @GetMapping("/resolve")
    @Operation(summary = "Resolves an api with User and Path gives target URL")
    public ResponseEntity<ApiResponse> resolve(@RequestParam("userId") Long userId, @RequestParam("path") String path) {
        String target = service.getByUserAndPath(userId, path).getTargetUrl();
        return ResponseEntity.ok(new ApiResponse(target));
    }
}