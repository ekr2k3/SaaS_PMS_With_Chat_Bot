
package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.config.StripeProperties;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class StripeService {

    private final StripeProperties stripeProperties;

    public StripeService(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
    }

    public Session createCheckoutSession(
            Long paymentId,
            String orderId,
            String requestId,
            String planName,
            Long amountInSmallestUnit,
            String currency,
            Long subscriptionId,
            Long planId
    ) throws StripeException {

        if (!StringUtils.hasText(stripeProperties.getSecretKey())) {
            throw new IllegalStateException(
                    "Stripe secret key chưa được cấu hình"
            );
        }

        if (!StringUtils.hasText(stripeProperties.getSuccessUrl())
                || !StringUtils.hasText(stripeProperties.getCancelUrl())) {
            throw new IllegalStateException(
                    "Stripe success URL/cancel URL chưa được cấu hình"
            );
        }

        Stripe.apiKey = stripeProperties.getSecretKey();

        String normalizedCurrency =
                currency.toLowerCase(Locale.ROOT);

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(stripeProperties.getSuccessUrl())
                        .setCancelUrl(stripeProperties.getCancelUrl())
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem
                                                        .PriceData.builder()
                                                        .setCurrency(normalizedCurrency)
                                                        .setUnitAmount(amountInSmallestUnit)
                                                        .setProductData(
                                                                SessionCreateParams
                                                                        .LineItem
                                                                        .PriceData
                                                                        .ProductData
                                                                        .builder()
                                                                        .setName(planName)
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .putMetadata(
                                "subscription_payment_id",
                                paymentId.toString()
                        )
                        .putMetadata("order_id", orderId)
                        .putMetadata(
                                "subscription_id",
                                subscriptionId.toString()
                        )
                        .putMetadata("plan_id", planId.toString())
                        .build();

        RequestOptions requestOptions =
                RequestOptions.builder()
                        .setIdempotencyKey(requestId)
                        .build();

        return Session.create(params, requestOptions);
    }
}