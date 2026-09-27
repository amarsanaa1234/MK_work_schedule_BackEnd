package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/** A workspace's plan, trial state, real subscription and usage, for the Plan and billing screen. */
@Getter
@AllArgsConstructor
public class PlanResponse {

    /** The plan actually in force: FREE, PRO or BUSINESS (FREE once a trial/subscription has lapsed). */
    private String plan;
    /** MONTHLY or YEARLY. */
    private String interval;
    private boolean onTrial;
    private LocalDate trialEndsOn;
    private int trialDaysLeft;
    private int trialLengthDays;
    private boolean trialUsed;
    /** A paid plan was chosen but neither the trial nor a subscription is currently active. */
    private boolean lapsed;
    private long peopleCount;
    private int maxPeople;
    private int workspacesUsed;
    private int maxWorkspaces;
    private long adminCount;
    private boolean multipleAdmins;
    /** A real Stripe subscription is active (as opposed to the no-card trial). */
    private boolean paymentMethodAdded;
    private String cardBrand;
    private String cardLast4;
    /** When the current paid billing period renews, if [paymentMethodAdded]. */
    private LocalDate currentPeriodEnd;
    /** Whether the server has Stripe configured at all - hides "Add payment method" when false. */
    private boolean paymentsEnabled;
    private int monthlyPrice;
    private int yearlyPrice;
    /** The plan has no people limit (Business); [maxPeople] is then a huge sentinel, not a real cap. */
    private boolean unlimitedPeople;
    /** The signed-in admin may add another workspace right now (Business, under the quota). */
    private boolean canAddWorkspace;
}
