package com.example.mk_backEnd.service;

import com.example.mk_backEnd.domain.PlanTier;
import com.example.mk_backEnd.domain.Workspace;
import com.example.mk_backEnd.dto.CheckoutResponse;

/**
 * Paid plans via Stripe Checkout (hosted page - card, Apple Pay and Google Pay are offered there
 * automatically). All of it is optional: with no secret key configured, {@link #isEnabled()} is
 * false and the app falls back to the no-card trial ({@link PlanService}) only.
 */
public interface StripeService {

    boolean isEnabled();

    /** Creates a Checkout Session for [tier]/[interval]; the caller opens the returned URL in a browser. */
    CheckoutResponse startCheckout(Workspace workspace, String customerEmail, String customerName,
                                    PlanTier tier, String interval, String successUrl, String cancelUrl);

    /**
     * Re-reads the workspace's subscription from Stripe and syncs plan/period/card. Used when the
     * app comes back from the browser, and by the webhook. Returns true if a subscription is active.
     */
    boolean syncFromStripe(Workspace workspace);

    /**
     * Applies a finished Checkout Session (the browser is redirected here after paying), so the
     * plan opens immediately even if the webhook hasn't arrived or isn't configured.
     */
    void completeCheckoutSession(String sessionId);

    /** Cancels the workspace's active subscription immediately (switching to Free). */
    void cancelSubscription(Workspace workspace);

    /** Verifies and applies one Stripe webhook delivery. */
    void handleWebhook(String payload, String signatureHeader);
}
