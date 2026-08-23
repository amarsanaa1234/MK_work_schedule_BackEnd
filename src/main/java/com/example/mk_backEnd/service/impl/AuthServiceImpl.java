package com.example.mk_backEnd.service.impl;

import com.example.mk_backEnd.domain.Employee;
import com.example.mk_backEnd.domain.User;
import com.example.mk_backEnd.domain.Workspace;
import com.example.mk_backEnd.dto.RegisterRequest;
import com.example.mk_backEnd.exception.BadRequestException;
import com.example.mk_backEnd.exception.ResourceNotFoundException;
import com.example.mk_backEnd.repository.UserRepository;
import com.example.mk_backEnd.repository.WorkspaceRepository;
import com.example.mk_backEnd.service.ActivityLogService;
import com.example.mk_backEnd.service.AuthService;
import com.example.mk_backEnd.service.FileStorageService;
import com.example.mk_backEnd.util.PasswordUtil;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ActivityLogService activityLogService;
    private final FileStorageService fileStorageService;

    public AuthServiceImpl(UserRepository userRepository, WorkspaceRepository workspaceRepository,
                            ActivityLogService activityLogService, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.activityLogService = activityLogService;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Хэрэглэгчийн нэр бүртгэлтэй байна: " + request.getUsername());
        }

        Workspace workspace = workspaceRepository.findByOrganizationId(request.getOrganizationId().toUpperCase())
                .orElseThrow(() -> new BadRequestException("Байгууллагын ID олдсонгүй: " + request.getOrganizationId()));

        User user = new Employee();
        user.setUsername(request.getUsername());
        user.setPasswordHash(PasswordUtil.hash(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setWorkspace(workspace);

        if (request.getAddressLine() != null && !request.getAddressLine().isBlank()) {
            var address = new com.example.mk_backEnd.domain.Address();
            address.setAddressLine(request.getAddressLine());
            address.setLatitude(request.getLatitude());
            address.setLongitude(request.getLongitude());
            user.setAddress(address);
        }

        if (request.getPhoto() != null && !request.getPhoto().isEmpty()) {
            user.setPhotoUrl(fileStorageService.store(request.getPhoto()));
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
