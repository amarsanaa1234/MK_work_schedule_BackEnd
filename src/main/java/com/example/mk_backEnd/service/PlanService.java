package com.example.mk_backEnd.service;

import com.example.mk_backEnd.domain.Admin;
import com.example.mk_backEnd.domain.PlanTier;
import com.example.mk_backEnd.domain.User;
import com.example.mk_backEnd.domain.Workspace;
import com.example.mk_backEnd.dto.PlanResponse;
import com.example.mk_backEnd.exception.BadRequestException;
import com.example.mk_backEnd.repository.AdminRepository;
import com.example.mk_backEnd.repository.EmployeeRepository;
import com.example.mk_backEnd.repository.UserRepository;
import com.example.mk_backEnd.repository.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Plans, trials, real Stripe billing, and the people limit.
 *
 * <p>A workspace gets access to a paid tier's limits two ways: the one-off 30-day no-card
 * trial ({@link #TRIAL_DAYS}), or a real Stripe subscription in good standing (see
 * {@link StripeService}). Whichever grants a higher tier wins. When neither is active the
 * workspace is FREE, and anyone beyond the Free tier's people limit (by join order) is
 * blocked from logging in - see {@link #seatAllowed} - until the workspace pays again, at
 * which point access resumes immediately since the check is always computed live.
 *
 * <p>A Business owner can run several workspaces. The plan belongs to the <em>owner</em>: every
 * workspace an admin owns shares the best tier any of them holds (see {@link #effectiveTier}), and
 * plan/billing screens always act on one stable "billing workspace" (see {@link #billingWorkspace})
 * no matter which of their workspaces the admin currently has open.
 */
@Service
public class PlanService {

    public static final int TRIAL_DAYS = 30;

    private final WorkspaceRepository workspaceRepository;
    private final EmployeeRepository employeeRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final StripeService stripeService;
    private final boolean multiWorkspaceRequiresPaid;

    public PlanService(WorkspaceRepository workspaceRepository, EmployeeRepository employeeRepository,
                       AdminRepository adminRepository, UserRepository userRepository,
                       StripeService stripeService,
                       @Value("${plans.multi-workspace.require-paid:false}") boolean multiWorkspaceRequiresPaid) {
        this.workspaceRepository = workspaceRepository;
        this.employeeRepository = employeeRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.stripeService = stripeService;
        this.multiWorkspaceRequiresPaid = multiWorkspaceRequiresPaid;
    }

    private boolean trialActive(Workspace workspace) {
        return workspace.getTrialEndsOn() != null && !LocalDate.now().isAfter(workspace.getTrialEndsOn());
    }

    /** A real, paid Stripe subscription that hasn't lapsed (webhooks/sync keep this current). */
    private boolean subscriptionActive(Workspace workspace) {
        return workspace.getStripeSubscriptionId() != null
                && Set.of("active", "trialing", "past_due").contains(workspace.getSubscriptionStatus())
                && workspace.getCurrentPeriodEnd() != null
                && !LocalDate.now().isAfter(workspace.getCurrentPeriodEnd());
    }

    /** The tier this one workspace holds on its own: a paid plan with neither a live trial nor a live subscription counts as FREE. */
    private PlanTier ownTier(Workspace workspace) {
        PlanTier chosen = PlanTier.parse(workspace.getPlan());
        return chosen != PlanTier.FREE && (trialActive(workspace) || subscriptionActive(workspace))
                ? chosen : PlanTier.FREE;
    }

    /** Every workspace the same owner runs, oldest first; just this one when it has no owner recorded. */
    public List<Workspace> ownedWorkspaces(Workspace workspace) {
        String owner = workspace.getOwnerAdminId();
        if (owner == null) {
            return List.of(workspace);
        }
        List<Workspace> owned = workspaceRepository.findByOwnerAdminIdOrderByCreatedAtAsc(owner);
        return owned.isEmpty() ? List.of(workspace) : owned;
    }

    /**
     * The tier actually in force. Workspaces with the same owner share the best tier any of them
     * holds, so adding a workspace under a Business plan doesn't need a second subscription.
     */
    public PlanTier effectiveTier(Workspace workspace) {
        PlanTier best = ownTier(workspace);
        if (workspace.getOwnerAdminId() == null) {
            return best;
        }
        for (Workspace w : ownedWorkspaces(workspace)) {
            PlanTier t = ownTier(w);
            if (t.ordinal() > best.ordinal()) {
                best = t;
            }
        }
        return best;
    }

    /**
     * The workspace that carries the trial/subscription for [workspace]'s owner: the one holding the
     * best tier, or - when they're all Free - the oldest, so the plan screen stays put as the admin
     * switches between their workspaces.
     */
    public Workspace billingWorkspace(Workspace workspace) {
        List<Workspace> owned = ownedWorkspaces(workspace);
        Workspace best = owned.get(0);
        PlanTier bestTier = ownTier(best);
        for (Workspace w : owned) {
            PlanTier t = ownTier(w);
            if (t.ordinal() > bestTier.ordinal()) {
                best = w;
                bestTier = t;
            }
        }
        return best;
    }

    /** Everyone in the workspace, crew and admins, counts toward the limit. */
    public long peopleCount(Workspace workspace) {
        return employeeRepository.countByWorkspaceIdAndRemovedAtIsNull(workspace.getId())
                + adminRepository.countMembers(workspace.getId());
    }

    /** People across all of an owner's workspaces (the owner counts once, not once per workspace). */
    public long totalPeople(Workspace workspace) {
        List<Workspace> owned = ownedWorkspaces(workspace);
        if (owned.size() == 1) {
            return peopleCount(owned.get(0));
        }
        long crew = 0;
        Set<String> adminIds = new HashSet<>();
        for (Workspace w : owned) {
            crew += employeeRepository.countByWorkspaceIdAndRemovedAtIsNull(w.getId());
            adminRepository.findMembers(w.getId()).forEach(a -> adminIds.add(a.getId()));
        }
        return crew + adminIds.size();
    }

    public boolean isFull(Workspace workspace) {
        return peopleCount(workspace) >= effectiveTier(workspace).getMaxPeople();
    }

    /**
     * Whether this specific person's seat is still covered by the workspace's current plan.
     * People are ranked by join order; the first [maxPeople] of them can log in and use the
     * app, the rest are locked out until the workspace upgrades or renews - at which point
     * this simply starts returning true for them again, no separate "reactivate" step needed.
     */
    public boolean seatAllowed(User user) {
        Workspace workspace = user.getWorkspace();
        PlanTier tier = effectiveTier(workspace);
        if (tier.isUnlimitedPeople()) {
            return true;
        }
        long rank = userRepository.countByWorkspaceIdAndRemovedAtIsNullAndCreatedAtLessThanEqual(
                workspace.getId(), user.getCreatedAt());
        return rank <= tier.getMaxPeople();
    }

    /** Whether this admin may add another workspace right now (Business only, under the quota). */
    public boolean canAddWorkspace(Workspace workspace) {
        PlanTier tier = effectiveTier(workspace);
        if (tier.getMaxWorkspaces() <= 1) {
            return false;
        }
        if (ownedWorkspaces(workspace).size() >= tier.getMaxWorkspaces()) {
            return false;
        }
        return !multiWorkspaceRequiresPaid || subscriptionActive(billingWorkspace(workspace));
    }

    /** Same check as {@link #canAddWorkspace}, but explains why not. */
    public void assertCanAddWorkspace(Admin admin) {
        Workspace workspace = admin.getWorkspace();
        PlanTier tier = effectiveTier(workspace);
        if (tier.getMaxWorkspaces() <= 1) {
            throw new BadRequestException(
                    "Running more than one workspace is part of the Business plan. Upgrade to Business to add another.");
        }
        if (ownedWorkspaces(workspace).size() >= tier.getMaxWorkspaces()) {
            throw new BadRequestException("You have used all " + tier.getMaxWorkspaces()
                    + " workspaces on your Business plan.");
        }
        if (multiWorkspaceRequiresPaid && !subscriptionActive(billingWorkspace(workspace))) {
            throw new BadRequestException(
                    "Adding a workspace needs a paid Business subscription. Add a payment method first.");
        }
    }

    public PlanResponse describe(Workspace workspace) {
        PlanTier chosen = PlanTier.parse(workspace.getPlan());
        PlanTier effective = effectiveTier(workspace);
        boolean onTrial = chosen != PlanTier.FREE && trialActive(workspace);
        boolean paying = chosen != PlanTier.FREE && subscriptionActive(workspace);
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
                chosen != PlanTier.FREE && !onTrial && !paying,
                totalPeople(workspace),
                effective.getMaxPeople(),
                ownedWorkspaces(workspace).size(),
                effective.getMaxWorkspaces(),
                adminRepository.countMembers(workspace.getId()),
                effective.isMultipleAdmins(),
                paying,
                workspace.getCardBrand(),
                workspace.getCardLast4(),
                workspace.getCurrentPeriodEnd(),
                stripeService.isEnabled(),
                effective.getMonthlyPrice(),
                effective.getYearlyPrice(),
                effective.isUnlimitedPeople(),
                canAddWorkspace(workspace));
    }

    @Transactional
    public PlanResponse changePlan(Workspace workspace, String plan, String interval) {
        PlanTier target = PlanTier.parse(plan);
        String billing = "YEARLY".equalsIgnoreCase(interval) ? "YEARLY" : "MONTHLY";

        if (target == PlanTier.FREE) {
            // Anyone already in the workspace stays; new sign-ups just pause until it is under the limit.
            if (workspace.getStripeSubscriptionId() != null) {
                stripeService.cancelSubscription(workspace);
            }
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
                    "Your free trial has already been used. Add a payment method to subscribe instead.");
        }

        return describe(workspaceRepository.save(workspace));
    }
}
