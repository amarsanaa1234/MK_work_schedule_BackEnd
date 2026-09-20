package com.example.mk_backEnd.service;

import com.example.mk_backEnd.domain.PlanTier;
import com.example.mk_backEnd.domain.Workspace;
import com.example.mk_backEnd.dto.PlanResponse;
import com.example.mk_backEnd.exception.BadRequestException;
import com.example.mk_backEnd.repository.AdminRepository;
import com.example.mk_backEnd.repository.EmployeeRepository;
import com.example.mk_backEnd.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Plans, trials and the people limit. There is no payment provider yet, so a paid plan can only
 * be held through its one 30-day no-card trial; when that ends the workspace falls back to FREE.
 */
@Service
public class PlanService {

    public static final int TRIAL_DAYS = 30;

    private final WorkspaceRepository workspaceRepository;
    private final EmployeeRepository employeeRepository;
    private final AdminRepository adminRepository;

    public PlanService(WorkspaceRepository workspaceRepository, EmployeeRepository employeeRepository,
                       AdminRepository adminRepository) {
        this.workspaceRepository = workspaceRepository;
        this.employeeRepository = employeeRepository;
        this.adminRepository = adminRepository;
    }

    private boolean trialActive(Workspace workspace) {
        return workspace.getTrialEndsOn() != null && !LocalDate.now().isAfter(workspace.getTrialEndsOn());
    }

    /** The tier actually in force: a paid plan whose trial has ended counts as FREE. */
    public PlanTier effectiveTier(Workspace workspace) {
        PlanTier chosen = PlanTier.parse(workspace.getPlan());
        return chosen != PlanTier.FREE && trialActive(workspace) ? chosen : PlanTier.FREE;
    }

    /** Everyone in the workspace, crew and admins, counts toward the limit. */
    public long peopleCount(Workspace workspace) {
        return employeeRepository.countByWorkspaceIdAndRemovedAtIsNull(workspace.getId())
                + adminRepository.countByWorkspaceId(workspace.getId());
    }

    public boolean isFull(Workspace workspace) {
        return peopleCount(workspace) >= effectiveTier(workspace).getMaxPeople();
    }

    public PlanResponse describe(Workspace workspace) {
        PlanTier chosen = PlanTier.parse(workspace.getPlan());
        PlanTier effective = effectiveTier(workspace);
        boolean onTrial = chosen != PlanTier.FREE && trialActive(workspace);
        int daysLeft = onTrial
                ? (int) Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), workspace.getTrialEndsOn()))
                : 0;

        return new PlanResponse(
                effective.name(),
                workspace.getBillingInterval() == null ? "MONTHLY" : workspace.getBillingInterval(),
                onTrial,
                workspace.getTrialEndsOn(),
                daysLeft,
                TRIAL_DAYS,
                workspace.getTrialStartedOn() != null,
                chosen != PlanTier.FREE && !onTrial,
                peopleCount(workspace),
                effective.getMaxPeople(),
                1,
                effective.getMaxWorkspaces(),
                adminRepository.countByWorkspaceId(workspace.getId()),
                effective.isMultipleAdmins(),
                false,
                effective.getMonthlyPrice(),
                effective.getYearlyPrice());
    }

    @Transactional
    public PlanResponse changePlan(Workspace workspace, String plan, String interval) {
        PlanTier target = PlanTier.parse(plan);
        String billing = "YEARLY".equalsIgnoreCase(interval) ? "YEARLY" : "MONTHLY";

        if (target == PlanTier.FREE) {
            // Anyone already in the workspace stays; new sign-ups just pause until it is under the limit.
            workspace.setPlan(PlanTier.FREE.name());
        } else if (trialActive(workspace) && PlanTier.parse(workspace.getPlan()) != PlanTier.FREE) {
            workspace.setPlan(target.name());
            workspace.setBillingInterval(billing);
        } else if (workspace.getTrialStartedOn() == null) {
            LocalDate today = LocalDate.now();
            workspace.setPlan(target.name());
            workspace.setBillingInterval(billing);
            workspace.setTrialStartedOn(today);
            workspace.setTrialEndsOn(today.plusDays(TRIAL_DAYS));
        } else {
            throw new BadRequestException(
                    "Your free trial has already been used, and card payments are not available yet.");
        }

        return describe(workspaceRepository.save(workspace));
    }
}
