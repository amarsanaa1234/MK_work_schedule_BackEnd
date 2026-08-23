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

    @PostMapping(value = "/register", consumes = "multipart/form-data")
    public ResponseEntity<LoginResponse> register(@Valid @ModelAttribute RegisterRequest request) {
        User user = authService.register(request);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), "EMPLOYEE");
        String organizationId = user.getWorkspace().getOrganizationId();
        LoginResponse response = new LoginResponse(
                true,
                user.getId(),
                "Employee",
                user.getFullName(),
                token,
                organizationId,
                user.getPhotoUrl(),
                user.getWorkspace().getIndustry(),
                user.getWorkspace().getAddress()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request.getUsername(), request.getPassword());
        String userType = user.getClass().getSimpleName();
        String role = userType.equalsIgnoreCase("Admin") ? "ADMIN" : "EMPLOYEE";
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), role);
        String organizationId = user.getWorkspace().getOrganizationId();
        return ResponseEntity.ok(new LoginResponse(
                true,
                user.getId(),
                userType,
                user.getFullName(),
                token,
                organizationId,
                user.getPhotoUrl(),
                user.getWorkspace().getIndustry(),
                user.getWorkspace().getAddress()
        ));
    }

    @PostMapping("/logout/{userId}")
    public ResponseEntity<Void> logout(@PathVariable String userId) {
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }
}
