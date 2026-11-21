
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.webhook;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/stripe/property")
public class StripePropertyWebhookController {

    private final StripePropertyWebhookService webhookService;

    public StripePropertyWebhookController(
            StripePropertyWebhookService webhookService
    ) {
        this.webhookService = webhookService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> receiveWebhook(
            @RequestBody String payload,
            @RequestHeader(
                    value = "Stripe-Signature",
                    required = false
            ) String signature
    ) {

        try {
            Map<String, Object> result =
                    webhookService.processWebhook(
                            payload,
                            signature
                    );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException exception) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "message",
                            exception.getMessage()
                    ));

        } catch (Exception exception) {

            // Không trả exception nội bộ hoặc credentials cho Stripe.
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message",
                            "Webhook processing failed"
                    ));
        }
    }
}