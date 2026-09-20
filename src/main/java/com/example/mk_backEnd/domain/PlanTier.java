package com.example.mk_backEnd.domain;

import com.example.mk_backEnd.exception.BadRequestException;
import lombok.Getter;

/** Subscription tiers and what each one allows. Prices are in AUD. */
@Getter
public enum PlanTier {

    FREE(10, 1, 0, 0, false),
    PRO(30, 1, 19, 190, true),
    BUSINESS(100, 5, 49, 490, true);

    private final int maxPeople;
    private final int maxWorkspaces;
    private final int monthlyPrice;
    private final int yearlyPrice;
    private final boolean multipleAdmins;

    PlanTier(int maxPeople, int maxWorkspaces, int monthlyPrice, int yearlyPrice, boolean multipleAdmins) {
        this.maxPeople = maxPeople;
        this.maxWorkspaces = maxWorkspaces;
        this.monthlyPrice = monthlyPrice;
        this.yearlyPrice = yearlyPrice;
        this.multipleAdmins = multipleAdmins;
    }

    /** Null or blank (workspaces created before plans existed) means FREE. */
    public static PlanTier parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return FREE;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown plan: " + raw);
        }
    }
}
