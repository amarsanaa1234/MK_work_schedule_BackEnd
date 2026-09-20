package com.example.mk_backEnd.controller;

import com.example.mk_backEnd.domain.Employee;
import com.example.mk_backEnd.domain.User;
import com.example.mk_backEnd.dto.UserProfileResponse;
import com.example.mk_backEnd.exception.ResourceNotFoundException;
import com.example.mk_backEnd.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The logged-in user's own profile — the "User profile" screen, for Admin and Employee alike. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(Authentication authentication) {
        User user = userRepository.findById(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Хэрэглэгч олдсонгүй"));

        Double payRate = user instanceof Employee employee ? employee.getPayRate() : null;

        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(),
                user.getClass().getSimpleName(),
                user.getFullName(),
                user.getUsername(),
                user.getPhone(),
                user.getPhotoUrl(),
                user.getCreatedAt(),
                payRate));
    }
}
