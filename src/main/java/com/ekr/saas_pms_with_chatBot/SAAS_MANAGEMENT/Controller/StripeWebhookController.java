package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Controller;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.StripeWebhookRouterService;

import com.stripe.exception.SignatureVerificationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {

    private final StripeWebhookRouterService stripeWebhookRouterService;

    public StripeWebhookController(
            StripeWebhookRouterService stripeWebhookRouterService
    ) {
        this.stripeWebhookRouterService = stripeWebhookRouterService;
    }

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(
                    value = "Stripe-Signature",
                    required = false
            ) String signature
    ) {

        if (signature == null || signature.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body("Missing Stripe-Signature");
        }

        try {
            stripeWebhookRouterService.handleWebhook(
                    payload,
                    signature
            );

            return ResponseEntity.ok("Webhook received");

        } catch (SignatureVerificationException exception) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid Stripe signature");

        } catch (IllegalArgumentException exception) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid webhook data");

        } catch (Exception exception) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Webhook processing failed");
        }
    }
}