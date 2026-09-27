package com.example.mk_backEnd.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
@Getter
@Setter
@NoArgsConstructor
public class Workspace {

    @Id
    private String id;

    @Column(name = "organization_id", unique = true, nullable = false, length = 8)
    private String organizationId;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    private String abn;

    private String industry;

    private String address;

    private String phone;

    /** FREE, PRO or BUSINESS as chosen; null on older workspaces means FREE. Text, so new tiers need no schema change. */
    @Column(name = "plan", length = 20)
    private String plan;

    /** MONTHLY or YEARLY. */
    @Column(name = "billing_interval", length = 20)
    private String billingInterval;

    @Column(name = "trial_started_on")
    private java.time.LocalDate trialStartedOn;

    @Column(name = "trial_ends_on")
    private java.time.LocalDate trialEndsOn;

    @Column(name = "stripe_customer_id", length = 40)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 40)
    private String stripeSubscriptionId;

    /** Mirrors the Stripe Subscription's status: incomplete, active, trialing, past_due, canceled... */
    @Column(name = "subscription_status", length = 20)
    private String subscriptionStatus;

    /** End of the current *paid* billing period. Distinct from the no-card [trialEndsOn]. */
    @Column(name = "current_period_end")
    private java.time.LocalDate currentPeriodEnd;

    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    /**
     * The admin who created (and pays for) this workspace. A Business owner can run several
     * workspaces, so the plan's workspace quota is counted per owner, not per workspace. Null only
     * on rows created before this column existed, until {@code WorkspaceOwnerBackfill} fills it in.
     */
    @Column(name = "owner_admin_id", length = 64)
    private String ownerAdminId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
    }
}
