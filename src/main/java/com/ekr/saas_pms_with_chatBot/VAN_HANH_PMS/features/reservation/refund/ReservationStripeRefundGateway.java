package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PaymentGatewayConfig;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PaymentGatewayConfigRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.security.StripeCredentialEncryptionService;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationPayment;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Component
public class ReservationStripeRefundGateway {
    private final PaymentGatewayConfigRepository configs;
    private final StripeCredentialEncryptionService encryption;
    public ReservationStripeRefundGateway(PaymentGatewayConfigRepository configs, StripeCredentialEncryptionService encryption) {
        this.configs=configs; this.encryption=encryption;
    }
    public Refund createRefund(ReservationPayment original, ReservationPayment refundPayment, String reason) throws StripeException {
        Long propertyId=original.getReservation().getProperty().getPropertyId();
        PaymentGatewayConfig config=configs.findByProperty_PropertyId(propertyId)
            .orElseThrow(() -> new IllegalStateException("Property has no Stripe configuration"));
        if(!"ACTIVE".equalsIgnoreCase(config.getStatus()) || config.getGateway()==null ||
                !"STRIPE".equalsIgnoreCase(config.getGateway().getName()) ||
                !"ACTIVE".equalsIgnoreCase(config.getGateway().getStatus()))
            throw new IllegalStateException("Property Stripe Gateway is not ACTIVE");
        String secret=encryption.decrypt(config.getSecretKey());
        String paymentIntentId=original.getPaymentIntentId();
        if(!StringUtils.hasText(paymentIntentId)) paymentIntentId=original.getTransactionId();
        if(!StringUtils.hasText(paymentIntentId)) throw new IllegalStateException("Original Payment has no Stripe PaymentIntent ID");
        long amount=toStripeAmount(refundPayment.getAmount(), refundPayment.getCurrency());
        Map<String,Object> params=new HashMap<>();
        params.put("payment_intent",paymentIntentId);
        params.put("amount",amount);
        Map<String,String> metadata=new HashMap<>();
        metadata.put("payment_flow","RESERVATION_REFUND");
        metadata.put("property_id",propertyId.toString());
        metadata.put("reservation_id",original.getReservation().getReservationId().toString());
        metadata.put("reservation_payment_id",original.getReservationPaymentId().toString());
        metadata.put("refund_payment_id",refundPayment.getReservationPaymentId().toString());
        if(StringUtils.hasText(reason)) metadata.put("reason",reason.substring(0,Math.min(reason.length(),500)));
        params.put("metadata",metadata);
        String stableKey = "reservation-refund-" + refundPayment.getReservationPaymentId();
        return Refund.create(params,RequestOptions.builder().setApiKey(secret).setIdempotencyKey(stableKey).build());
    }

    private long toStripeAmount(BigDecimal amount,String currency) {
        String code=currency.toUpperCase(Locale.ROOT);
        if("VND".equals(code) || "JPY".equals(code) || "KRW".equals(code))
            return amount.setScale(0,RoundingMode.UNNECESSARY).longValueExact();
        return amount.movePointRight(2).setScale(0,RoundingMode.UNNECESSARY).longValueExact();
    }
}
