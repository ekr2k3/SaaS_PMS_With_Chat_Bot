package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.webhook;

import com.ekr.saas_pms_with_chatBot.config.StripeProperties;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
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
     * Retrieve PaymentIntent được Stripe Checkout tạo ra.
     * Không tạo giao dịch thanh toán mới.
     */
    public PaymentIntent retrievePaymentIntent(
            String paymentIntentId
    ) throws StripeException {

        if (!StringUtils.hasText(stripeProperties.getSecretKey())) {
            throw new IllegalStateException(
                    "Stripe secret key chưa được cấu hình"
            );
        }

        if (!StringUtils.hasText(paymentIntentId)) {
            throw new IllegalArgumentException(
                    "PaymentIntent ID không hợp lệ"
            );
        }

        Stripe.apiKey = stripeProperties.getSecretKey();

        return PaymentIntent.retrieve(paymentIntentId);
    }
}
