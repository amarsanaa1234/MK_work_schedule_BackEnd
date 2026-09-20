package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/** A workspace's plan, trial state and usage, for the Plan and billing screen. */
@Getter
@AllArgsConstructor
public class PlanResponse {

    /** The plan actually in force: FREE, PRO or BUSINESS (FREE once a trial has lapsed). */
    private String plan;
    /** MONTHLY or YEARLY. */
    private String interval;
    private boolean onTrial;
    private LocalDate trialEndsOn;
    private int trialDaysLeft;
    private int trialLengthDays;
    private boolean trialUsed;
    /** A paid plan was chosen but its trial ended without a payment method, so it fell back to FREE. */
    private boolean lapsed;
    private long peopleCount;
    private int maxPeople;
    private int workspacesUsed;
    private int maxWorkspaces;
    private long adminCount;
    private boolean multipleAdmins;
    private boolean paymentMethodAdded;
    private int monthlyPrice;
    private int yearlyPrice;
}
