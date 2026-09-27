package com.example.mk_backEnd.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** A Stripe Checkout page the app opens in the browser to take payment for a subscription. */
@Getter
@AllArgsConstructor
public class CheckoutResponse {

    private String url;
    private String sessionId;
}
