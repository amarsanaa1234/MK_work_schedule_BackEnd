package com.example.mk_backEnd.controller;

import com.example.mk_backEnd.domain.Admin;
import com.example.mk_backEnd.domain.PlanTier;
import com.example.mk_backEnd.domain.Workspace;
import com.example.mk_backEnd.dto.ChangePlanRequest;
import com.example.mk_backEnd.dto.CheckoutResponse;
import com.example.mk_backEnd.dto.PlanResponse;
import com.example.mk_backEnd.exception.ResourceNotFoundException;
import com.example.mk_backEnd.repository.AdminRepository;
import com.example.mk_backEnd.service.PlanService;
import com.example.mk_backEnd.service.StripeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/admins/{adminId}/plan")
public class PlanController {

    private final PlanService planService;
    private final StripeService stripeService;
    private final AdminRepository adminRepository;

    public PlanController(PlanService planService, StripeService stripeService, AdminRepository adminRepository) {
        this.planService = planService;
        this.stripeService = stripeService;
        this.adminRepository = adminRepository;
    }

    private Admin adminOf(Authentication authentication, String adminId) {
        if (!authentication.getName().equals(adminId)) {
            throw new AccessDeniedException("Өөр админы эрхээр хандах боломжгүй");
        }
        return adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Админ олдсонгүй: " + adminId));
    }

    /**
     * The workspace that carries the plan for this admin's owner. Admins who run several workspaces
     * see and change one shared plan, whichever workspace they currently have open.
     */
    private Workspace workspaceOf(Authentication authentication, String adminId) {
        return planService.billingWorkspace(adminOf(authentication, adminId).getWorkspace());
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

    /** Creates a Stripe Checkout page for the app to open in the browser. */
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(Authentication authentication, @PathVariable String adminId,
                                                      @Valid @RequestBody ChangePlanRequest request) {
        Admin admin = adminOf(authentication, adminId);
        PlanTier tier = PlanTier.parse(request.getPlan());
        String interval = "YEARLY".equalsIgnoreCase(request.getInterval()) ? "YEARLY" : "MONTHLY";
        // Same host the app used to reach the API, so the phone's browser can reach the return page too.
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return ResponseEntity.ok(stripeService.startCheckout(
                planService.billingWorkspace(admin.getWorkspace()), admin.getUsername(), admin.getFullName(),
                tier, interval,
                base + "/api/stripe/return/success?session_id={CHECKOUT_SESSION_ID}",
                base + "/api/stripe/return/cancel"));
    }

    /** Called when the app returns from the browser, so the plan refreshes without waiting on the webhook. */
    @PostMapping("/confirm")
    public ResponseEntity<PlanResponse> confirm(Authentication authentication, @PathVariable String adminId) {
        Workspace workspace = workspaceOf(authentication, adminId);
        stripeService.syncFromStripe(workspace);
        return ResponseEntity.ok(planService.describe(workspace));
    }
}
