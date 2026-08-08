package com.example.mk_backEnd.service.impl;

import com.example.mk_backEnd.domain.Admin;
import com.example.mk_backEnd.domain.Employee;
import com.example.mk_backEnd.domain.User;
import com.example.mk_backEnd.dto.RegisterRequest;
import com.example.mk_backEnd.exception.BadRequestException;
import com.example.mk_backEnd.exception.ResourceNotFoundException;
import com.example.mk_backEnd.repository.UserRepository;
import com.example.mk_backEnd.service.ActivityLogService;
import com.example.mk_backEnd.service.AuthService;
import com.example.mk_backEnd.util.PasswordUtil;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    public AuthServiceImpl(UserRepository userRepository, ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
    }

    @Override
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Хэрэглэгчийн нэр бүртгэлтэй байна: " + request.getUsername());
        }

        User user = switch (request.getRole().toUpperCase()) {
            case "ADMIN" -> new Admin();
            case "EMPLOYEE" -> new Employee();
            default -> throw new BadRequestException("Тодорхойгүй эрх: " + request.getRole());
        };

        user.setUsername(request.getUsername());
        user.setPasswordHash(PasswordUtil.hash(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());

        if (request.getAddressLine() != null && !request.getAddressLine().isBlank()) {
            var address = new com.example.mk_backEnd.domain.Address();
            address.setAddressLine(request.getAddressLine());
            address.setLatitude(request.getLatitude());
            address.setLongitude(request.getLongitude());
            user.setAddress(address);
        }

        return userRepository.save(user);
    }

    @Override
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Хэрэглэгчийн нэр эсвэл нууц үг буруу байна."));

        if (!PasswordUtil.matches(password, user.getPasswordHash())) {
            throw new BadRequestException("Хэрэглэгчийн нэр эсвэл нууц үг буруу байна.");
        }

        activityLogService.log(user.getId(), "LOGIN", null);
        return user;
    }

    @Override
    public void logout(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Хэрэглэгч олдсонгүй: " + userId);
        }
        activityLogService.log(userId, "LOGOUT", null);
    }
}
