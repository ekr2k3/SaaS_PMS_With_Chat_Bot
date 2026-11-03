package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.checkout;

import com.ekr.saas_pms_with_chatBot.config.StripeProperties;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class ReservationStripeCheckoutGateway {

    private final StripeProperties stripeProperties;

    public ReservationStripeCheckoutGateway(
            StripeProperties stripeProperties
    ) {
        this.stripeProperties = stripeProperties;
    }


    /**
     * Tạo Checkout Session dành riêng cho Reservation.
     *
     * Không sử dụng StripeService của Subscription.
     */
    public Session createCheckoutSession(
            Long reservationPaymentId,
            Long reservationId,
            String reservationCode,
            String productName,
            long amount,
            String currency,
            String idempotencyKey
    ) throws StripeException {

        validateStripeConfiguration();

        if (reservationPaymentId == null
                || reservationId == null
                || !StringUtils.hasText(reservationCode)
                || !StringUtils.hasText(productName)
                || amount <= 0
                || !StringUtils.hasText(currency)
                || !StringUtils.hasText(idempotencyKey)) {

            throw new IllegalArgumentException(
                    "Thông tin tạo Reservation Checkout không hợp lệ"
            );
        }

        Stripe.apiKey = stripeProperties.getSecretKey();

        SessionCreateParams params =
                SessionCreateParams.builder()

                        .setMode(
                                SessionCreateParams.Mode.PAYMENT
                        )

                        .setSuccessUrl(
                                stripeProperties.getSuccessUrl()
                        )

                        .setCancelUrl(
                                stripeProperties.getCancelUrl()
                        )

                        .setClientReferenceId(
                                "RESERVATION_PAYMENT_"
                                        + reservationPaymentId
                        )

                        .addLineItem(
                                SessionCreateParams.LineItem.builder()

                                        .setQuantity(1L)

                                        .setPriceData(
                                                SessionCreateParams
                                                        .LineItem
                                                        .PriceData
                                                        .builder()

                                                        .setCurrency(
                                                                currency.toLowerCase(
                                                                        Locale.ROOT
                                                                )
                                                        )

                                                        .setUnitAmount(amount)

                                                        .setProductData(
                                                                SessionCreateParams
                                                                        .LineItem
                                                                        .PriceData
                                                                        .ProductData
                                                                        .builder()
                                                                        .setName(productName)
                                                                        .build()
                                                        )

                                                        .build()
                                        )

                                        .build()
                        )

                        // Metadata riêng cho Reservation Checkout Session.
                        .putMetadata(
                                "payment_flow",
                                "RESERVATION"
                        )

                        .putMetadata(
                                "reservation_payment_id",
                                reservationPaymentId.toString()
                        )

                        .putMetadata(
                                "reservation_id",
                                reservationId.toString()
                        )

                        .putMetadata(
                                "reservation_code",
                                reservationCode
                        )

                        // Metadata truyền tiếp vào PaymentIntent.
                        .setPaymentIntentData(
                                SessionCreateParams.PaymentIntentData.builder()
                                        .putMetadata("payment_flow", "RESERVATION")
                                        .putMetadata(
                                                "reservation_payment_id",
                                                reservationPaymentId.toString()
                                        )
                                        .putMetadata("reservation_id", reservationId.toString())
                                        .putMetadata("reservation_code", reservationCode)
                                        .build()
                        )

                        .build();

        RequestOptions requestOptions =
                RequestOptions.builder()
                        .setIdempotencyKey(idempotencyKey)
                        .build();

        return Session.create(
                params,
                requestOptions
        );
    }


    /**
     * Lấy lại Checkout Session đã tạo trước đó.
     */
    public Session retrieveCheckoutSession(
            String checkoutSessionId
    ) throws StripeException {

        validateStripeConfiguration();

        if (!StringUtils.hasText(checkoutSessionId)) {
            throw new IllegalArgumentException(
                    "Checkout Session ID không hợp lệ"
            );
        }

        Stripe.apiKey = stripeProperties.getSecretKey();

        return Session.retrieve(checkoutSessionId);
    }


    private void validateStripeConfiguration() {

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
    }
}
