package com.example.mk_backEnd.controller;

import com.example.mk_backEnd.service.StripeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Where Stripe Checkout sends the browser after paying or cancelling. Public - see SecurityConfig. */
@RestController
public class StripeReturnController {

    private final StripeService stripeService;

    public StripeReturnController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @GetMapping(value = "/api/stripe/return/success", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public String success(@RequestParam("session_id") String sessionId) {
        boolean ok = true;
        try {
            stripeService.completeCheckoutSession(sessionId);
        } catch (RuntimeException e) {
            ok = false;
        }
        return ok
                ? page("Payment successful", "Your plan is active. You can close this tab and go back to the app.")
                : page("Payment received", "We're still confirming it. Go back to the app and pull down to refresh in a moment.");
    }

    @GetMapping(value = "/api/stripe/return/cancel", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public String cancel() {
        return page("Payment cancelled", "Nothing was charged. You can close this tab and go back to the app.");
    }

    private static String page(String title, String message) {
        return "<!doctype html><html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>" + title + "</title>"
                + "<style>body{font-family:system-ui,sans-serif;background:#1b1e23;color:#eee;display:flex;"
                + "align-items:center;justify-content:center;min-height:100vh;margin:0;text-align:center;padding:24px}"
                + "h1{font-size:22px}p{color:#aab}</style></head><body><div><h1>" + title + "</h1><p>" + message
                + "</p></div></body></html>";
    }
}
