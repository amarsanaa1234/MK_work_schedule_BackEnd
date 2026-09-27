package com.example.mk_backEnd.service.impl;

import com.example.mk_backEnd.domain.PlanTier;
import com.example.mk_backEnd.domain.Workspace;
import com.example.mk_backEnd.dto.CheckoutResponse;
import com.example.mk_backEnd.exception.BadRequestException;
import com.example.mk_backEnd.repository.WorkspaceRepository;
import com.example.mk_backEnd.service.StripeService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Price;
import com.stripe.model.PriceCollection;
import com.stripe.model.Product;
import com.stripe.model.StripeObject;
import com.stripe.model.SubscriptionCollection;
import com.stripe.model.checkout.Session;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.PriceListParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.ProductUpdateParams;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stripe Checkout billing. The hosted Checkout page offers card, Apple Pay and Google Pay itself, so the
 * server only creates sessions and reads subscription state back.
 *
 * <p>Subscription entitlement is read live from Stripe on every {@link #syncFromStripe} /
 * webhook call rather than cached locally beyond the fields needed to show the billing screen,
 * so a payment made from the Stripe dashboard or a renewal charge is picked up automatically.
 */
@Slf4j
@Service
public class StripeServiceImpl implements StripeService {

    /** Subscription statuses that still grant access — "past_due" is a grace period, Stripe is retrying the card. */
    private static final Set<String> ACTIVE_STATUSES = Set.of("active", "trialing", "past_due");

    private final WorkspaceRepository workspaceRepository;
    private final String secretKey;
    private final String webhookSecret;

    private final Map<String, String> priceIds = new ConcurrentHashMap<>();
    private volatile boolean enabled = false;

    public StripeServiceImpl(WorkspaceRepository workspaceRepository,
                              @Value("${stripe.secret-key:}") String secretKey,
                              @Value("${stripe.webhook-secret:}") String webhookSecret) {
        this.workspaceRepository = workspaceRepository;
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
    }

    @PostConstruct
    void init() {
        if (secretKey == null || secretKey.isBlank()) {
            log.info("Stripe secret key not set - real payments are disabled; only the no-card trial is available.");
            return;
        }
        Stripe.apiKey = secretKey;
        try {
            ensurePrices();
            enabled = true;
            log.info("Stripe payments enabled ({} price(s) ready).", priceIds.size());
        } catch (StripeException e) {
            log.warn("Could not set up Stripe products/prices - payments stay disabled: {}", e.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private void requireEnabled() {
        if (!enabled) {
            throw new BadRequestException("Payments are not set up on this server yet.");
        }
    }

    // ---- one-time product/price provisioning -----------------------------------------------

    private void ensurePrices() throws StripeException {
        for (PlanTier tier : new PlanTier[]{PlanTier.PRO, PlanTier.BUSINESS}) {
            priceIds.put(priceKey(tier, "MONTHLY"), findOrCreatePrice(tier, "MONTHLY", tier.getMonthlyPrice()));
            priceIds.put(priceKey(tier, "YEARLY"), findOrCreatePrice(tier, "YEARLY", tier.getYearlyPrice()));
        }
    }

    private static String priceKey(PlanTier tier, String interval) {
        return tier.name() + "_" + interval.toUpperCase();
    }

    private static String lookupKeyFor(PlanTier tier, String interval) {
        return "mkroster_" + tier.name().toLowerCase() + "_" + interval.toLowerCase();
    }

    private static String productName(PlanTier tier, String interval) {
        return "Muster " + capitalize(tier.name()) + " (" + capitalize(interval) + ")";
    }

    /** Reuses an existing Price by lookup key if one exists, so restarts never create duplicates. */
    private String findOrCreatePrice(PlanTier tier, String interval, int amountInDollars) throws StripeException {
        String lookupKey = lookupKeyFor(tier, interval);
        PriceCollection existing = Price.list(
                PriceListParams.builder().addLookupKey(lookupKey).setActive(true).build());
        if (!existing.getData().isEmpty()) {
            Price found = existing.getData().get(0);
            // Keeps products created under an older name in step with the current one.
            Product product = Product.retrieve(found.getProduct());
            if (!productName(tier, interval).equals(product.getName())) {
                product.update(ProductUpdateParams.builder().setName(productName(tier, interval)).build());
            }
            return found.getId();
        }

        Product product = Product.create(ProductCreateParams.builder()
                .setName(productName(tier, interval))
                .putMetadata("app", "mk_roster")
                .build());

        PriceCreateParams.Recurring.Interval stripeInterval = "YEARLY".equalsIgnoreCase(interval)
                ? PriceCreateParams.Recurring.Interval.YEAR
                : PriceCreateParams.Recurring.Interval.MONTH;

        Price price = Price.create(PriceCreateParams.builder()
                .setProduct(product.getId())
                .setCurrency("aud")
                .setUnitAmount((long) amountInDollars * 100)
                .setLookupKey(lookupKey)
                .setRecurring(PriceCreateParams.Recurring.builder().setInterval(stripeInterval).build())
                .build());
        return price.getId();
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // ---- checkout ----------------------------------------------------------------------------

    @Override
    @Transactional
    public CheckoutResponse startCheckout(Workspace workspace, String customerEmail, String customerName,
                                           PlanTier tier, String interval, String successUrl, String cancelUrl) {
        requireEnabled();
        try {
            Customer customer = getOrCreateCustomer(workspace, customerEmail, customerName);
            workspace.setStripeCustomerId(customer.getId());
            workspaceRepository.save(workspace);

            String priceId = priceIds.get(priceKey(tier, interval.toUpperCase()));
            if (priceId == null) {
                throw new BadRequestException("No Stripe price is set up for " + tier + " " + interval + " yet.");
            }

            // No payment_method_types on purpose: the hosted page then offers whatever is enabled in the
            // Stripe dashboard (card, plus Apple Pay / Google Pay on devices that support them).
            Session session = Session.create(SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customer.getId())
                    .addLineItem(SessionCreateParams.LineItem.builder().setPrice(priceId).setQuantity(1L).build())
                    .setClientReferenceId(workspace.getId())
                    .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                            .putMetadata("workspace_id", workspace.getId())
                            .build())
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .build());

            return new CheckoutResponse(session.getUrl(), session.getId());
        } catch (StripeException e) {
            log.warn("Stripe checkout failed for workspace {}: {}", workspace.getId(), e.getMessage());
            throw new BadRequestException("Could not start checkout with Stripe: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void completeCheckoutSession(String sessionId) {
        if (!enabled) {
            return;
        }
        try {
            Session session = Session.retrieve(sessionId);
            String workspaceId = session.getClientReferenceId();
            Workspace workspace = workspaceId == null ? null : workspaceRepository.findById(workspaceId).orElse(null);
            if (workspace == null) {
                throw new BadRequestException("Checkout session doesn't match a workspace.");
            }
            syncFromStripe(workspace);
        } catch (StripeException e) {
            throw new BadRequestException("Could not confirm payment with Stripe: " + e.getMessage());
        }
    }

    private Customer getOrCreateCustomer(Workspace workspace, String email, String name) throws StripeException {
        if (workspace.getStripeCustomerId() != null) {
            try {
                Customer existing = Customer.retrieve(workspace.getStripeCustomerId());
                if (!Boolean.TRUE.equals(existing.getDeleted())) {
                    return existing;
                }
            } catch (StripeException e) {
                log.info("Stripe customer {} for workspace {} is gone, creating a new one.",
                        workspace.getStripeCustomerId(), workspace.getId());
            }
        }
        return Customer.create(CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name)
                .putMetadata("workspace_id", workspace.getId())
                .putMetadata("organization_id", workspace.getOrganizationId())
                .build());
    }

    // ---- syncing entitlement -------------------------------------------------------------------

    @Override
    @Transactional
    public boolean syncFromStripe(Workspace workspace) {
        if (!enabled) {
            return false;
        }
        try {
            // Checkout creates the subscription on Stripe's side, so find it through the customer
            // rather than an id we stored up front.
            Subscription best = null;
            if (workspace.getStripeCustomerId() != null) {
                SubscriptionCollection subs = Subscription.list(SubscriptionListParams.builder()
                        .setCustomer(workspace.getStripeCustomerId()).build());
                for (Subscription s : subs.getData()) {
                    if (ACTIVE_STATUSES.contains(s.getStatus())
                            && (best == null || s.getCreated() > best.getCreated())) {
                        best = s;
                    }
                }
                // Switching plans starts a second subscription; keep only the newest so nobody is billed twice.
                for (Subscription s : subs.getData()) {
                    if (best != null && !s.getId().equals(best.getId()) && ACTIVE_STATUSES.contains(s.getStatus())) {
                        try {
                            s.cancel();
                        } catch (StripeException e) {
                            log.info("Could not cancel older subscription {}: {}", s.getId(), e.getMessage());
                        }
                    }
                }
            }
            if (best != null) {
                workspace.setStripeSubscriptionId(best.getId());
                return applySubscriptionState(workspace, best);
            }
            if (workspace.getStripeSubscriptionId() != null) {
                return applySubscriptionState(workspace, Subscription.retrieve(workspace.getStripeSubscriptionId()));
            }
            return false;
        } catch (StripeException e) {
            throw new BadRequestException("Could not confirm payment with Stripe: " + e.getMessage());
        }
    }

    /** Applies a Subscription's status/period/price/card onto the workspace. Reused by sync + webhook. */
    private boolean applySubscriptionState(Workspace workspace, Subscription subscription) {
        workspace.setSubscriptionStatus(subscription.getStatus());
        boolean active = ACTIVE_STATUSES.contains(subscription.getStatus());

        if (active) {
            var item = subscription.getItems().getData().isEmpty() ? null : subscription.getItems().getData().get(0);
            Long periodEndEpoch = item == null ? null : item.getCurrentPeriodEnd();
            if (periodEndEpoch != null) {
                workspace.setCurrentPeriodEnd(Instant.ofEpochSecond(periodEndEpoch).atZone(ZoneOffset.UTC).toLocalDate());
            }
            String lookupKey = item == null || item.getPrice() == null ? null : item.getPrice().getLookupKey();
            applyTierFromLookupKey(workspace, lookupKey);
            syncPaymentMethod(workspace, subscription);
        } else {
            // canceled / unpaid / incomplete_expired: no grace period left, back to Free.
            workspace.setPlan(PlanTier.FREE.name());
            workspace.setCurrentPeriodEnd(null);
        }

        workspaceRepository.save(workspace);
        return active;
    }

    /** A lookup key of "mkroster_pro_yearly" tells us the tier/interval without a reverse map. */
    private void applyTierFromLookupKey(Workspace workspace, String lookupKey) {
        if (lookupKey == null || !lookupKey.startsWith("mkroster_")) {
            return;
        }
        String[] parts = lookupKey.substring("mkroster_".length()).split("_");
        if (parts.length == 2) {
            workspace.setPlan(parts[0].toUpperCase());
            workspace.setBillingInterval(parts[1].toUpperCase());
        }
    }

    private void syncPaymentMethod(Workspace workspace, Subscription subscription) {
        String paymentMethodId = subscription.getDefaultPaymentMethod();
        if (paymentMethodId == null) {
            return;
        }
        try {
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            PaymentMethod.Card card = paymentMethod.getCard();
            if (card != null) {
                workspace.setCardBrand(card.getBrand());
                workspace.setCardLast4(card.getLast4());
            }
        } catch (StripeException e) {
            log.info("Could not read card details for workspace {}: {}", workspace.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public void cancelSubscription(Workspace workspace) {
        if (!enabled || workspace.getStripeSubscriptionId() == null) {
            return;
        }
        try {
            Subscription.retrieve(workspace.getStripeSubscriptionId()).cancel();
        } catch (StripeException e) {
            log.info("Stripe subscription {} already gone for workspace {}: {}",
                    workspace.getStripeSubscriptionId(), workspace.getId(), e.getMessage());
        }
        workspace.setSubscriptionStatus("canceled");
        workspace.setCurrentPeriodEnd(null);
    }

    // ---- webhook -------------------------------------------------------------------------------

    @Override
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new BadRequestException("Stripe webhook received, but no webhook secret is configured.");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new BadRequestException("Invalid Stripe webhook signature.");
        }

        String type = event.getType();
        if (type == null || !type.startsWith("customer.subscription.")) {
            return; // not something entitlement depends on
        }

        Optional<StripeObject> dataObject = event.getDataObjectDeserializer().getObject();
        if (dataObject.isEmpty() || !(dataObject.get() instanceof Subscription subscription)) {
            log.info("Stripe webhook {} arrived without a usable Subscription payload; skipping.", type);
            return;
        }

        Workspace workspace = resolveWorkspace(subscription);
        if (workspace == null) {
            log.info("Stripe webhook {} for subscription {} matches no local workspace; skipping.",
                    type, subscription.getId());
            return;
        }

        if ("customer.subscription.deleted".equals(type)) {
            workspace.setPlan(PlanTier.FREE.name());
            workspace.setSubscriptionStatus(subscription.getStatus());
            workspace.setCurrentPeriodEnd(null);
            workspaceRepository.save(workspace);
        } else {
            applySubscriptionState(workspace, subscription);
        }
    }

    private Workspace resolveWorkspace(Subscription subscription) {
        Map<String, String> metadata = subscription.getMetadata();
        String workspaceId = metadata == null ? null : metadata.get("workspace_id");
        if (workspaceId != null) {
            Optional<Workspace> byId = workspaceRepository.findById(workspaceId);
            if (byId.isPresent()) {
                return byId.get();
            }
        }
        return workspaceRepository.findByStripeSubscriptionId(subscription.getId()).orElse(null);
    }
}
