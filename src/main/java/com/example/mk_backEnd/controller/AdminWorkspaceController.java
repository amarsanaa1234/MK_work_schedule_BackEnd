package com.example.mk_backEnd.controller;

import com.example.mk_backEnd.domain.Admin;
import com.example.mk_backEnd.domain.Workspace;
import com.example.mk_backEnd.dto.AddWorkspaceRequest;
import com.example.mk_backEnd.dto.LoginResponse;
import com.example.mk_backEnd.dto.WorkspaceSummaryResponse;
import com.example.mk_backEnd.exception.ResourceNotFoundException;
import com.example.mk_backEnd.repository.AdminRepository;
import com.example.mk_backEnd.security.JwtUtil;
import com.example.mk_backEnd.service.AdminService;
import com.example.mk_backEnd.service.LoginResponseFactory;
import com.example.mk_backEnd.service.PlanService;
import com.example.mk_backEnd.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** The workspaces one admin runs: list them, add another (Business), and switch which one is open. */
@RestController
@RequestMapping("/api/admins/{adminId}/workspaces")
public class AdminWorkspaceController {

    private final WorkspaceService workspaceService;
    private final AdminService adminService;
    private final AdminRepository adminRepository;
    private final PlanService planService;
    private final JwtUtil jwtUtil;
    private final LoginResponseFactory loginResponses;

    public AdminWorkspaceController(WorkspaceService workspaceService, AdminService adminService,
                                    AdminRepository adminRepository, PlanService planService, JwtUtil jwtUtil,
                                    LoginResponseFactory loginResponses) {
        this.workspaceService = workspaceService;
        this.adminService = adminService;
        this.adminRepository = adminRepository;
        this.planService = planService;
        this.jwtUtil = jwtUtil;
        this.loginResponses = loginResponses;
    }

    private Admin adminOf(Authentication authentication, String adminId) {
        if (!authentication.getName().equals(adminId)) {
            throw new AccessDeniedException("Өөр админы эрхээр хандах боломжгүй");
        }
        return adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Админ олдсонгүй: " + adminId));
    }

    private WorkspaceSummaryResponse summaryOf(Workspace workspace, Admin admin, LocalDate from, LocalDate to) {
        Integer unpaid = null;
        Integer missing = null;
        if (from != null && to != null) {
            AdminService.WorkspaceAttention attention = adminService.attentionFor(workspace, from, to);
            unpaid = attention.unpaid();
            missing = attention.missingLogs();
        }
        return new WorkspaceSummaryResponse(
                workspace.getOrganizationId(),
                workspace.getBusinessName(),
                workspace.getAddress(),
                workspace.getIndustry(),
                planService.peopleCount(workspace),
                workspace.getId().equals(admin.getWorkspace().getId()),
                unpaid,
                missing);
    }

    /** Pass a pay period as [from]/[to] to also get each workspace's unpaid and missing-log counts. */
    @GetMapping
    public ResponseEntity<List<WorkspaceSummaryResponse>> list(
            Authentication authentication,
            @PathVariable String adminId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Admin admin = adminOf(authentication, adminId);
        return ResponseEntity.ok(workspaceService.workspacesOf(admin).stream()
                .map(w -> summaryOf(w, admin, from, to))
                .toList());
    }

    @PostMapping
    public ResponseEntity<WorkspaceSummaryResponse> add(Authentication authentication,
                                                        @PathVariable String adminId,
                                                        @Valid @RequestBody AddWorkspaceRequest request) {
        Admin admin = adminOf(authentication, adminId);
        Workspace created = workspaceService.addWorkspace(adminId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(summaryOf(created, admin, null, null));
    }

    /** Opens another of the admin's workspaces; the response is the refreshed session for the app to keep. */
    @PostMapping("/{organizationId}/switch")
    public ResponseEntity<LoginResponse> switchTo(Authentication authentication,
                                                  @PathVariable String adminId,
                                                  @PathVariable String organizationId) {
        adminOf(authentication, adminId);
        Admin admin = workspaceService.switchWorkspace(adminId, organizationId);
        String token = jwtUtil.generateToken(admin.getId(), admin.getUsername(), "ADMIN");
        return ResponseEntity.ok(loginResponses.build(admin, "Admin", token));
    }
}
