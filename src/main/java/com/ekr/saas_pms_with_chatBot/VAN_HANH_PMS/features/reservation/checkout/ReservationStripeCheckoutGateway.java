
package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.checkout;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PaymentGatewayConfig;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PaymentGatewayConfigRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.security.StripeCredentialEncryptionService;
import com.ekr.saas_pms_with_chatBot.config.StripeProperties;

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
    private final PaymentGatewayConfigRepository configRepository;
    private final StripeCredentialEncryptionService encryptionService;

    public ReservationStripeCheckoutGateway(
            StripeProperties stripeProperties,
            PaymentGatewayConfigRepository configRepository,
            StripeCredentialEncryptionService encryptionService
    ) {
        this.stripeProperties = stripeProperties;
        this.configRepository = configRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * Tạo Checkout Session bằng Secret Key của Property.
     */
    public Session createCheckoutSession(
            Long propertyId,
            Long reservationPaymentId,
            Long reservationId,
            String reservationCode,
            String productName,
            long amount,
            String currency,
            String idempotencyKey
    ) throws StripeException {

        validateCheckoutUrls();

        if (propertyId == null
                || reservationPaymentId == null
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

        String propertySecretKey = resolvePropertySecretKey(propertyId);

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(stripeProperties.getSuccessUrl())
                        .setCancelUrl(stripeProperties.getCancelUrl())
                        .setClientReferenceId(
                                "RESERVATION_PAYMENT_" + reservationPaymentId
                        )
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency(
                                                                currency.toLowerCase(Locale.ROOT)
                                                        )
                                                        .setUnitAmount(amount)
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName(productName)
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .putMetadata("payment_flow", "RESERVATION")
                        .putMetadata(
                                "reservation_payment_id",
                                reservationPaymentId.toString()
                        )
                        .putMetadata(
                                "reservation_id",
                                reservationId.toString()
                        )
                        .putMetadata("reservation_code", reservationCode)
                        .setPaymentIntentData(
                                SessionCreateParams.PaymentIntentData.builder()
                                        .putMetadata("payment_flow", "RESERVATION")
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
                                        .build()
                        )
                        .build();

        // API key được gắn riêng cho request này.
        // Không thay đổi Stripe.apiKey toàn cục.
        RequestOptions requestOptions =
                RequestOptions.builder()
                        .setApiKey(propertySecretKey)
                        .setIdempotencyKey(idempotencyKey)
                        .build();

        return Session.create(params, requestOptions);
    }

    /**
     * Lấy lại Checkout Session bằng Secret Key của Property.
     */
    public Session retrieveCheckoutSession(
            Long propertyId,
            String checkoutSessionId
    ) throws StripeException {

        if (propertyId == null
                || !StringUtils.hasText(checkoutSessionId)) {

            throw new IllegalArgumentException(
                    "Property ID hoặc Checkout Session ID không hợp lệ"
            );
        }

        String propertySecretKey = resolvePropertySecretKey(propertyId);

        RequestOptions requestOptions =
                RequestOptions.builder()
                        .setApiKey(propertySecretKey)
                        .build();

        return Session.retrieve(checkoutSessionId, requestOptions);
    }

    /**
     * Lấy Secret Key từ cấu hình Payment Gateway của Property.
     *
     * Không fallback về Stripe Secret Key SaaS.
     */
    private String resolvePropertySecretKey(Long propertyId) {

        PaymentGatewayConfig config = configRepository
                .findByProperty_PropertyId(propertyId)
                .orElseThrow(() -> new IllegalStateException(
                        "Property chưa cấu hình Stripe Payment Gateway"
                ));

        if (!"ACTIVE".equalsIgnoreCase(config.getStatus())) {
            throw new IllegalStateException(
                    "Stripe Payment Gateway của Property chưa ACTIVE"
            );
        }

        if (config.getGateway() == null
                || !"STRIPE".equalsIgnoreCase(config.getGateway().getName())
                || !"ACTIVE".equalsIgnoreCase(config.getGateway().getStatus())) {

            throw new IllegalStateException(
                    "Stripe Gateway không hợp lệ hoặc chưa ACTIVE"
            );
        }

        if (!StringUtils.hasText(config.getSecretKey())) {
            throw new IllegalStateException(
                    "Property chưa cấu hình Stripe Secret Key"
            );
        }

        String secretKey = encryptionService.decrypt(config.getSecretKey());

        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalStateException(
                    "Không thể giải mã Stripe Secret Key của Property"
            );
        }

        return secretKey;
    }

    /**
     * Success URL và Cancel URL là URL callback của ứng dụng.
     * Chúng vẫn được dùng chung từ cấu hình StripeProperties.
     */
    private void validateCheckoutUrls() {

        if (!StringUtils.hasText(stripeProperties.getSuccessUrl())
                || !StringUtils.hasText(stripeProperties.getCancelUrl())) {

            throw new IllegalStateException(
                    "Stripe success URL/cancel URL chưa được cấu hình"
            );
        }
    }
}