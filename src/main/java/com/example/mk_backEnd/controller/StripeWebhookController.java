package com.example.mk_backEnd.controller;

import com.example.mk_backEnd.service.StripeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stripe calls this directly (not through the app) whenever a subscription changes -
 * renewals, failed/retried payments, cancellations, or a change made from the Stripe
 * dashboard. This is what keeps a workspace's plan correct without anyone having the app
 * open. Authenticated by the Stripe-Signature header, not a JWT - see SecurityConfig.
 */
@RestController
public class StripeWebhookController {

    private final StripeService stripeService;

    public StripeWebhookController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/api/stripe/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                         @RequestHeader("Stripe-Signature") String signature) {
        stripeService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
