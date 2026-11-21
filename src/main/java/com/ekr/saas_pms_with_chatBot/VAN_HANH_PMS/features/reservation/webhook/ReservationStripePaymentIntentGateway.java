
package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.webhook;

import com.ekr.saas_pms_with_chatBot.config.StripeProperties;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReservationStripePaymentIntentGateway {

    private final StripeProperties stripeProperties;

    public ReservationStripePaymentIntentGateway(
            StripeProperties stripeProperties
    ) {
        this.stripeProperties = stripeProperties;
    }

    /**
     * Retrieve PaymentIntent bằng Secret Key SaaS.
     *
     * Giữ lại để tương thích với luồng Webhook Reservation cũ.
     */
    public PaymentIntent retrievePaymentIntent(
            String paymentIntentId
    ) throws StripeException {

        validatePaymentIntentId(paymentIntentId);

        String secretKey = stripeProperties.getSecretKey();

        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalStateException(
                    "Stripe SaaS Secret Key chưa được cấu hình"
            );
        }

        RequestOptions options = RequestOptions.builder()
                .setApiKey(secretKey)
                .build();

        return PaymentIntent.retrieve(paymentIntentId, options);
    }

    /**
     * Retrieve PaymentIntent bằng Secret Key riêng của Property.
     */
    public PaymentIntent retrievePaymentIntent(
            String paymentIntentId,
            String propertySecretKey
    ) throws StripeException {

        validatePaymentIntentId(paymentIntentId);

        if (!StringUtils.hasText(propertySecretKey)) {
            throw new IllegalArgumentException(
                    "Stripe Property Secret Key không hợp lệ"
            );
        }

        RequestOptions options = RequestOptions.builder()
                .setApiKey(propertySecretKey)
                .build();

        return PaymentIntent.retrieve(paymentIntentId, options);
    }

    private void validatePaymentIntentId(String paymentIntentId) {

        if (!StringUtils.hasText(paymentIntentId)) {
            throw new IllegalArgumentException(
                    "PaymentIntent ID không hợp lệ"
            );
        }
    }
}