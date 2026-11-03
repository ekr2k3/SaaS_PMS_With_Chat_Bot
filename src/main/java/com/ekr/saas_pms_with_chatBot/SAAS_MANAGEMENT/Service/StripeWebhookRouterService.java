package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.webhook.ReservationStripeWebhookService;
import com.ekr.saas_pms_with_chatBot.config.StripeProperties;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

@Service
public class StripeWebhookRouterService {

    private final StripeProperties stripeProperties;

    private final ReservationStripeWebhookService reservationWebhookService;

    private final StripeWebhookService subscriptionWebhookService;

    public StripeWebhookRouterService(
            StripeProperties stripeProperties,
            ReservationStripeWebhookService reservationWebhookService,
            StripeWebhookService subscriptionWebhookService
    ) {
        this.stripeProperties = stripeProperties;
        this.reservationWebhookService = reservationWebhookService;
        this.subscriptionWebhookService = subscriptionWebhookService;
    }

    public void handleWebhook(
            String payload,
            String signature
    ) throws SignatureVerificationException {

        if (!StringUtils.hasText(stripeProperties.getWebhookSecret())) {
            throw new IllegalStateException(
                    "Stripe webhook secret chưa được cấu hình"
            );
        }

        if (!StringUtils.hasText(signature)) {
            throw new IllegalArgumentException(
                    "Thiếu Stripe-Signature"
            );
        }

        Event event = Webhook.constructEvent(
                payload,
                signature,
                stripeProperties.getWebhookSecret()
        );

        if (isReservationCheckoutEvent(event)) {

            reservationWebhookService.handleVerifiedEvent(event);

            return;
        }

        // Giữ nguyên xử lý Subscription hiện tại.
        // Service cũ tự xác minh signature lần nữa.
        subscriptionWebhookService.handleWebhook(
                payload,
                signature
        );
    }

    private boolean isReservationCheckoutEvent(Event event) {

        String type = event.getType();

        if (!"checkout.session.completed".equals(type)
                && !"checkout.session.async_payment_succeeded".equals(type)
                && !"checkout.session.async_payment_failed".equals(type)
                && !"checkout.session.expired".equals(type)) {

            return false;
        }

        Optional<StripeObject> object =
                event.getDataObjectDeserializer().getObject();

        if (object.isEmpty()
                || !(object.get() instanceof Session session)) {

            return false;
        }

        Map<String, String> metadata = session.getMetadata();

        return metadata != null
                && "RESERVATION".equals(
                        metadata.get("payment_flow")
                );
    }
}
