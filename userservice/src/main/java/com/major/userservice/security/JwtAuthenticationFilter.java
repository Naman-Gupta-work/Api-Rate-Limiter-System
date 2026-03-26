package com.major.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        System.out.println("1. Auth Header: " + authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                String email = jwtUtil.extractEmail(token);
                System.out.println("2. Extracted Email: " + email);
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    boolean isValid = jwtUtil.isTokenValid(token);
                    System.out.println("3. Is Token Valid? " + isValid);
                    if (isValid) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, null, new ArrayList<>());
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        System.out.println("4. SUCCESS: User authenticated in Security Context!");
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }else{
            System.out.println("X-RAY: No valid Bearer token found in header.");
        }

        chain.doFilter(request, response);
    }
}