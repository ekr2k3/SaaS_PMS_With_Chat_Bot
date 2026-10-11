package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Controller;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.StripeWebhookService;

import com.stripe.exception.SignatureVerificationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    public StripeWebhookController(
            StripeWebhookService stripeWebhookService
    ) {
        this.stripeWebhookService = stripeWebhookService;
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
            stripeWebhookService.handleWebhook(payload, signature);

            return ResponseEntity.ok("Webhook received");

        } catch (SignatureVerificationException exception) {

            // Chữ ký không hợp lệ: không xử lý nghiệp vụ.
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid Stripe signature");

        } catch (IllegalArgumentException exception) {

            /*
             * Dữ liệu không hợp lệ hoặc không khớp với Payment.
             * Không xác nhận thanh toán.
             */
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid webhook data");

        } catch (Exception exception) {

            /*
             * Lỗi hệ thống hoặc database:
             * trả lỗi 5xx để Stripe có thể retry.
             */
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Webhook processing failed");
        }
    }
}
