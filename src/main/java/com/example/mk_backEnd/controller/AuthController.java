package com.example.mk_backEnd.controller;

import com.example.mk_backEnd.domain.User;
import com.example.mk_backEnd.dto.LoginRequest;
import com.example.mk_backEnd.dto.LoginResponse;
import com.example.mk_backEnd.dto.RegisterRequest;
import com.example.mk_backEnd.security.JwtUtil;
import com.example.mk_backEnd.service.AuthService;
import com.example.mk_backEnd.service.LoginResponseFactory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final LoginResponseFactory loginResponses;

    public AuthController(AuthService authService, JwtUtil jwtUtil, LoginResponseFactory loginResponses) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.loginResponses = loginResponses;
    }

    @PostMapping(value = "/register", consumes = "multipart/form-data")
    public ResponseEntity<LoginResponse> register(@Valid @ModelAttribute RegisterRequest request) {
        User user = authService.register(request);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), "EMPLOYEE");
        return ResponseEntity.status(HttpStatus.CREATED).body(loginResponses.build(user, "Employee", token));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request.getUsername(), request.getPassword());
        String userType = user.getClass().getSimpleName();
        String role = userType.equalsIgnoreCase("Admin") ? "ADMIN" : "EMPLOYEE";
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), role);
        return ResponseEntity.ok(loginResponses.build(user, userType, token));
    }

    @PostMapping("/logout/{userId}")
    public ResponseEntity<Void> logout(@PathVariable String userId) {
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }
}
