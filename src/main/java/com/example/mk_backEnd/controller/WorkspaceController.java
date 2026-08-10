package com.example.mk_backEnd.controller;

import com.example.mk_backEnd.domain.Admin;
import com.example.mk_backEnd.domain.Workspace;
import com.example.mk_backEnd.dto.CreateWorkspaceRequest;
import com.example.mk_backEnd.dto.LoginResponse;
import com.example.mk_backEnd.dto.WorkspaceLookupResponse;
import com.example.mk_backEnd.security.JwtUtil;
import com.example.mk_backEnd.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final JwtUtil jwtUtil;

    public WorkspaceController(WorkspaceService workspaceService, JwtUtil jwtUtil) {
        this.workspaceService = workspaceService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<LoginResponse> createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request) {
        WorkspaceService.WorkspaceAndAdmin result = workspaceService.createWorkspace(request);
        Admin admin = result.admin();
        Workspace workspace = result.workspace();

        String token = jwtUtil.generateToken(admin.getId(), admin.getUsername(), "ADMIN");
        LoginResponse response = new LoginResponse(
                true, admin.getId(), "Admin", admin.getFullName(), token, workspace.getOrganizationId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{organizationId}")
    public ResponseEntity<WorkspaceLookupResponse> lookup(@PathVariable String organizationId) {
        Workspace workspace = workspaceService.findByOrganizationId(organizationId);
        return ResponseEntity.ok(new WorkspaceLookupResponse(
                workspace.getOrganizationId(), workspace.getBusinessName(), workspace.getAddress()));
    }
}
