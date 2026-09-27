package com.example.mk_backEnd.service.impl;

import com.example.mk_backEnd.domain.Admin;
import com.example.mk_backEnd.domain.User;
import com.example.mk_backEnd.domain.Workspace;
import com.example.mk_backEnd.dto.AddWorkspaceRequest;
import com.example.mk_backEnd.dto.CreateWorkspaceRequest;
import com.example.mk_backEnd.exception.BadRequestException;
import com.example.mk_backEnd.exception.ResourceNotFoundException;
import com.example.mk_backEnd.repository.AdminRepository;
import com.example.mk_backEnd.repository.UserRepository;
import com.example.mk_backEnd.repository.WorkspaceRepository;
import com.example.mk_backEnd.service.FileStorageService;
import com.example.mk_backEnd.service.PlanService;
import com.example.mk_backEnd.service.WorkspaceService;
import com.example.mk_backEnd.util.OrganizationIdGenerator;
import com.example.mk_backEnd.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final OrganizationIdGenerator organizationIdGenerator;
    private final FileStorageService fileStorageService;
    private final PlanService planService;

    public WorkspaceServiceImpl(WorkspaceRepository workspaceRepository, UserRepository userRepository,
                                 AdminRepository adminRepository, OrganizationIdGenerator organizationIdGenerator,
                                 FileStorageService fileStorageService, PlanService planService) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.organizationIdGenerator = organizationIdGenerator;
        this.fileStorageService = fileStorageService;
        this.planService = planService;
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
        admin.setPhone(request.getPhone());
        admin.setWorkspace(workspace);

        if (request.getPhoto() != null && !request.getPhoto().isEmpty()) {
            admin.setPhotoUrl(fileStorageService.store(request.getPhoto()));
        }

        Admin savedAdmin = (Admin) userRepository.save(admin);

        // The admin who signs a business up owns it (and any further workspaces they add later).
        workspace.setOwnerAdminId(savedAdmin.getId());
        workspaceRepository.save(workspace);

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

    @Override
    public List<Workspace> workspacesOf(Admin admin) {
        Workspace current = admin.getWorkspace();
        List<Workspace> owned = new ArrayList<>(workspaceRepository.findByOwnerAdminIdOrderByCreatedAtAsc(admin.getId()));
        if (owned.stream().noneMatch(w -> w.getId().equals(current.getId()))) {
            owned.add(0, current);
        }
        return owned;
    }

    @Override
    public int workspaceCountFor(User user) {
        return user instanceof Admin admin ? workspacesOf(admin).size() : 1;
    }

    @Override
    @Transactional
    public Workspace addWorkspace(String adminId, AddWorkspaceRequest request) {
        Admin admin = findAdmin(adminId);
        Workspace current = admin.getWorkspace();

        String owner = current.getOwnerAdminId();
        if (owner != null && !owner.equals(admin.getId())) {
            throw new BadRequestException("Only the owner of this business can add another workspace.");
        }
        planService.assertCanAddWorkspace(admin);

        // Legacy rows may not have an owner yet - the admin adding a workspace is the owner.
        if (owner == null) {
            current.setOwnerAdminId(admin.getId());
            workspaceRepository.save(current);
        }

        Workspace workspace = new Workspace();
        workspace.setOrganizationId(organizationIdGenerator.generate());
        workspace.setBusinessName(request.getBusinessName().trim());
        workspace.setAbn(request.getAbn());
        workspace.setIndustry(request.getIndustry());
        workspace.setAddress(request.getAddress());
        workspace.setOwnerAdminId(admin.getId());
        return workspaceRepository.save(workspace);
    }

    @Override
    @Transactional
    public Admin switchWorkspace(String adminId, String organizationId) {
        Admin admin = findAdmin(adminId);
        Workspace target = findByOrganizationId(organizationId);
        boolean allowed = workspacesOf(admin).stream().anyMatch(w -> w.getId().equals(target.getId()));
        if (!allowed) {
            throw new BadRequestException("You don't have access to that workspace.");
        }
        admin.setWorkspace(target);
        return adminRepository.save(admin);
    }

    private Admin findAdmin(String adminId) {
        return adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Админ олдсонгүй: " + adminId));
    }
}
