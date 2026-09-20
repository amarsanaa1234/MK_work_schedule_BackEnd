package com.example.mk_backEnd.controller;

import com.example.mk_backEnd.domain.Workspace;
import com.example.mk_backEnd.dto.ChangePlanRequest;
import com.example.mk_backEnd.dto.PlanResponse;
import com.example.mk_backEnd.exception.ResourceNotFoundException;
import com.example.mk_backEnd.repository.AdminRepository;
import com.example.mk_backEnd.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admins/{adminId}/plan")
public class PlanController {

    private final PlanService planService;
    private final AdminRepository adminRepository;

    public PlanController(PlanService planService, AdminRepository adminRepository) {
        this.planService = planService;
        this.adminRepository = adminRepository;
    }

    private Workspace workspaceOf(Authentication authentication, String adminId) {
        if (!authentication.getName().equals(adminId)) {
            throw new AccessDeniedException("Өөр админы эрхээр хандах боломжгүй");
        }
        return adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Админ олдсонгүй: " + adminId))
                .getWorkspace();
    }

    @GetMapping
    public ResponseEntity<PlanResponse> get(Authentication authentication, @PathVariable String adminId) {
        return ResponseEntity.ok(planService.describe(workspaceOf(authentication, adminId)));
    }

    @PutMapping
    public ResponseEntity<PlanResponse> change(Authentication authentication, @PathVariable String adminId,
                                               @Valid @RequestBody ChangePlanRequest request) {
        return ResponseEntity.ok(planService.changePlan(
                workspaceOf(authentication, adminId), request.getPlan(), request.getInterval()));
    }
}
