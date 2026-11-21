
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.webhook;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PaymentGatewayConfig;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PaymentGatewayConfigRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.security.StripeCredentialEncryptionService;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.webhook.ReservationStripeWebhookService;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund.ReservationStripeRefundWebhookService;
import com.stripe.model.Refund;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StripePropertyWebhookService {

    private final PaymentGatewayConfigRepository configRepository;

    private final StripeCredentialEncryptionService encryptionService;

    private final ReservationStripeWebhookService reservationWebhookService;
    private final ReservationStripeRefundWebhookService refundWebhookService;

    public StripePropertyWebhookService(
            PaymentGatewayConfigRepository configRepository,
            StripeCredentialEncryptionService encryptionService,
            ReservationStripeWebhookService reservationWebhookService,
            ReservationStripeRefundWebhookService refundWebhookService
    ) {
        this.configRepository = configRepository;
        this.encryptionService = encryptionService;
        this.reservationWebhookService = reservationWebhookService;
        this.refundWebhookService = refundWebhookService;
    }

    /**
     * Xác minh Webhook bằng Webhook Secret của Property.
     * Nếu Event thuộc Reservation Checkout thì chuyển sang xử lý nghiệp vụ.
     */
    @Transactional
    public Map<String, Object> processWebhook(
            String payload,
            String signature
    ) {

        if (!StringUtils.hasText(payload)) {
            throw new IllegalArgumentException(
                    "Webhook payload không được để trống"
            );
        }

        if (!StringUtils.hasText(signature)) {
            throw new IllegalArgumentException(
                    "Thiếu Stripe-Signature"
            );
        }

        List<PaymentGatewayConfig> configs =
                configRepository.findAllByStatus("ACTIVE");

        if (configs.isEmpty()) {
            throw new IllegalArgumentException(
                    "Không có cấu hình Payment Gateway ACTIVE"
            );
        }

        for (PaymentGatewayConfig config : configs) {

            if (!isActiveStripeConfig(config)) {
                continue;
            }

            if (!StringUtils.hasText(config.getWebhookSecret())) {
                continue;
            }

            String webhookSecret;

            try {
                webhookSecret = encryptionService.decrypt(
                        config.getWebhookSecret()
                );
            } catch (RuntimeException exception) {
                // Không ghi credentials vào log.
                continue;
            }

            Event event;

            try {
                event = Webhook.constructEvent(
                        payload,
                        signature,
                        webhookSecret
                );

            } catch (SignatureVerificationException exception) {
                // Thử Webhook Secret của Property tiếp theo.
                continue;
            }

            // Từ đây đã xác định được Property gửi Webhook.
            Long propertyId = config.getProperty().getPropertyId();

            if (propertyId == null) {
                throw new IllegalStateException(
                        "Payment Gateway Config không liên kết Property"
                );
            }

            if (isReservationCheckoutEvent(event)) {
                String propertySecretKey = decryptPropertySecret(config);
                reservationWebhookService.handleVerifiedPropertyEvent(
                        event, propertyId, propertySecretKey
                );
            } else if (isReservationRefundEvent(event)) {
                refundWebhookService.handleVerifiedPropertyEvent(event, propertyId);
            }

            Map<String, Object> result = new LinkedHashMap<>();

            result.put("message", "Webhook processed");
            result.put("propertyId", propertyId);
            result.put("gatewayConfigId", config.getGatewayConfigId());
            result.put("eventId", event.getId());
            result.put("eventType", event.getType());

            return result;
        }

        throw new IllegalArgumentException(
                "Stripe signature không khớp với cấu hình Property nào"
        );
    }

    private String decryptPropertySecret(PaymentGatewayConfig config) {
        String secret = encryptionService.decrypt(config.getSecretKey());
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("Property Stripe Secret Key không hợp lệ");
        }
        return secret;
    }

    private boolean isReservationRefundEvent(Event event) {
        if (!"refund.created".equals(event.getType())
                && !"refund.updated".equals(event.getType())) return false;
        Optional<StripeObject> object = event.getDataObjectDeserializer().getObject();
        if (object.isEmpty() || !(object.get() instanceof Refund refund)) {
            throw new IllegalArgumentException("Không thể đọc Stripe Refund từ Event");
        }
        Map<String,String> metadata = refund.getMetadata();
        return metadata != null && "RESERVATION_REFUND".equals(metadata.get("payment_flow"));
    }

    /**
     * Chỉ nhận cấu hình Stripe đang ACTIVE.
     */
    private boolean isActiveStripeConfig(
            PaymentGatewayConfig config
    ) {

        return config.getGateway() != null
                && config.getProperty() != null
                && "STRIPE".equalsIgnoreCase(
                config.getGateway().getName()
        )
                && "ACTIVE".equalsIgnoreCase(
                config.getGateway().getStatus()
        );
    }

    /**
     * Chỉ chuyển các Event Checkout Session liên quan đến Reservation.
     * Các Event Stripe khác được bỏ qua an toàn.
     */
    private boolean isReservationCheckoutEvent(Event event) {

        String eventType = event.getType();

        if (!"checkout.session.completed".equals(eventType)
                && !"checkout.session.async_payment_succeeded".equals(eventType)
                && !"checkout.session.async_payment_failed".equals(eventType)
                && !"checkout.session.expired".equals(eventType)) {

            return false;
        }

        Optional<StripeObject> object =
                event.getDataObjectDeserializer().getObject();

        if (object.isEmpty()
                || !(object.get() instanceof Session session)) {

            throw new IllegalArgumentException(
                    "Không thể đọc Checkout Session từ Stripe Event"
            );
        }

        Map<String, String> metadata = session.getMetadata();

        return metadata != null
                && "RESERVATION".equals(
                metadata.get("payment_flow")
        );
    }
}