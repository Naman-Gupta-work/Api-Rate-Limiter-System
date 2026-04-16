package com.major.gateway.controller;


import com.major.gateway.service.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> getLiveAnalytics(
            @RequestParam(value = "path", defaultValue = "all") String path,
            @RequestAttribute("validatedUserId") Long userId) {

        // Using @RequestAttribute is cleaner than manually pulling from HttpServletRequest
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // This call is now "Blocking" but runs on a Virtual Thread (Zero overhead)
        Map<String, Object> data = analyticsService.getDashboardData(userId, path);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/summary")
    public ResponseEntity<Object> getSummary(@RequestAttribute("validatedUserId") Long userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> data = analyticsService.getDashboardData(userId, "all");
        return ResponseEntity.ok(data.get("summary"));
    }
}