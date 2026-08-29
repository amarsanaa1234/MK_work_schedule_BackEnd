package com.example.mk_backEnd.service.impl;

import com.example.mk_backEnd.domain.Admin;
import com.example.mk_backEnd.domain.Workspace;
import com.example.mk_backEnd.dto.CreateWorkspaceRequest;
import com.example.mk_backEnd.exception.BadRequestException;
import com.example.mk_backEnd.exception.ResourceNotFoundException;
import com.example.mk_backEnd.repository.UserRepository;
import com.example.mk_backEnd.repository.WorkspaceRepository;
import com.example.mk_backEnd.service.FileStorageService;
import com.example.mk_backEnd.service.WorkspaceService;
import com.example.mk_backEnd.util.OrganizationIdGenerator;
import com.example.mk_backEnd.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final OrganizationIdGenerator organizationIdGenerator;
    private final FileStorageService fileStorageService;

    public WorkspaceServiceImpl(WorkspaceRepository workspaceRepository, UserRepository userRepository,
                                 OrganizationIdGenerator organizationIdGenerator,
                                 FileStorageService fileStorageService) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.organizationIdGenerator = organizationIdGenerator;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public WorkspaceAndAdmin createWorkspace(CreateWorkspaceRequest request) {
        if (userRepository.existsByUsername(request.getAdminEmail())) {
            throw new BadRequestException("Энэ имэйлээр аль хэдийн бүртгэлтэй байна: " + request.getAdminEmail());
        }

        Workspace workspace = new Workspace();
        workspace.setOrganizationId(organizationIdGenerator.generate());
        workspace.setBusinessName(request.getBusinessName());
        workspace.setAbn(request.getAbn());
        workspace.setIndustry(request.getIndustry());
        workspace.setAddress(request.getAddress());
        workspace = workspaceRepository.save(workspace);

        Admin admin = new Admin();
        admin.setUsername(request.getAdminEmail());
        admin.setPasswordHash(PasswordUtil.hash(request.getAdminPassword()));
        admin.setFullName(request.getAdminName());
        admin.setWorkspace(workspace);

        if (request.getPhoto() != null && !request.getPhoto().isEmpty()) {
            admin.setPhotoUrl(fileStorageService.store(request.getPhoto()));
        }

        Admin savedAdmin = (Admin) userRepository.save(admin);

        return new WorkspaceAndAdmin(workspace, savedAdmin);
    }

    @Override
    public Workspace findByOrganizationId(String organizationId) {
        return workspaceRepository.findByOrganizationId(organizationId.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Байгууллагын ID олдсонгүй: " + organizationId));
    }

    @Override
    public Workspace findByUserId(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Хэрэглэгч олдсонгүй: " + userId))
                .getWorkspace();
    }
}
