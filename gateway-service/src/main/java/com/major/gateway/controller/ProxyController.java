package com.major.gateway.controller;

import com.major.gateway.service.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/proxy")
public class ProxyController {

    private final ProxyService proxyService;

    public ProxyController(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> forward(
            HttpServletRequest request,
            @RequestAttribute(value = "validatedUserId", required = false) Long userId,
            @RequestBody(required = false) byte[] body
    ) {
        return proxyService.forwardRequest(request, userId, body);
    }
}