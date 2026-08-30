package com.example.mk_backEnd.controller;

import com.example.mk_backEnd.domain.Admin;
import com.example.mk_backEnd.domain.Workspace;
import com.example.mk_backEnd.dto.CreateWorkspaceRequest;
import com.example.mk_backEnd.dto.EmployeeSummaryResponse;
import com.example.mk_backEnd.dto.LoginResponse;
import com.example.mk_backEnd.dto.WorkspaceLookupResponse;
import com.example.mk_backEnd.dto.WorkspaceProfileResponse;
import com.example.mk_backEnd.security.JwtUtil;
import com.example.mk_backEnd.service.EmployeeService;
import com.example.mk_backEnd.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final EmployeeService employeeService;
    private final JwtUtil jwtUtil;

    public WorkspaceController(WorkspaceService workspaceService, EmployeeService employeeService, JwtUtil jwtUtil) {
        this.workspaceService = workspaceService;
        this.employeeService = employeeService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<LoginResponse> createWorkspace(@Valid @ModelAttribute CreateWorkspaceRequest request) {
        WorkspaceService.WorkspaceAndAdmin result = workspaceService.createWorkspace(request);
        Admin admin = result.admin();
        Workspace workspace = result.workspace();

        String token = jwtUtil.generateToken(admin.getId(), admin.getUsername(), "ADMIN");
        LoginResponse response = new LoginResponse(
                true,
                admin.getId(),
                "Admin",
                admin.getFullName(),
                token,
                workspace.getOrganizationId(),
                admin.getPhotoUrl(),
                admin.getWorkspace().getIndustry(),
                admin.getWorkspace().getAddress()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{organizationId}")
    public ResponseEntity<WorkspaceLookupResponse> lookup(@PathVariable String organizationId) {
        Workspace workspace = workspaceService.findByOrganizationId(organizationId);
        return ResponseEntity.ok(new WorkspaceLookupResponse(
                workspace.getOrganizationId(), workspace.getBusinessName(), workspace.getAddress()));
    }

    /**
     * Токеноор баталгаажсан хэрэглэгчийн харьяалагдах байгууллагын бүрэн мэдээллийг буцаана.
     * organizationId биш, JWT-ийн эзэмшигчээр тодорхойлогддог тул зөвхөн өөрийн байгууллагаа харна.
     */
    @GetMapping("/me")
    public ResponseEntity<WorkspaceProfileResponse> me(Authentication authentication) {
        Workspace workspace = workspaceService.findByUserId(authentication.getName());
        return ResponseEntity.ok(new WorkspaceProfileResponse(
                workspace.getOrganizationId(),
                workspace.getBusinessName(),
                workspace.getAbn(),
                workspace.getIndustry(),
                workspace.getAddress(),
                workspace.getPhone()
        ));
    }

    @GetMapping("/getEmployees")
    public ResponseEntity<List<EmployeeSummaryResponse>> getEmployees(Authentication authentication) {
        Workspace workspace = workspaceService.findByUserId(authentication.getName());
        List<EmployeeSummaryResponse> employees = employeeService.getAllEmployees(workspace.getId());
        return ResponseEntity.ok(employees);
    }
}
