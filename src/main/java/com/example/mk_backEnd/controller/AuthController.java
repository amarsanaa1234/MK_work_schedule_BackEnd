package com.example.mk_backEnd.controller;

import com.example.mk_backEnd.domain.User;
import com.example.mk_backEnd.dto.LoginRequest;
import com.example.mk_backEnd.dto.LoginResponse;
import com.example.mk_backEnd.dto.RegisterRequest;
import com.example.mk_backEnd.security.JwtUtil;
import com.example.mk_backEnd.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request.getUsername(), request.getPassword());
        String userType = user.getClass().getSimpleName();
        String role = userType.equalsIgnoreCase("Admin") ? "ADMIN" : "EMPLOYEE";
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), role);
        return ResponseEntity.ok(new LoginResponse(true, user.getId(), userType, user.getFullName(), token));
    }

    @PostMapping("/logout/{userId}")
    public ResponseEntity<Void> logout(@PathVariable String userId) {
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }
}
