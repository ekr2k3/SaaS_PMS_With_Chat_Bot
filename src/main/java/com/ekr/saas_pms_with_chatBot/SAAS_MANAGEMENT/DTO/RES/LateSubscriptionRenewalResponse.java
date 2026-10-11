package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.RES;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class LateSubscriptionRenewalResponse {

    private Long paymentId;
    private String orderId;
    private PaymentStatus paymentStatus;
    private String paymentUrl;
    private BigDecimal amount;
    private String currency;
}
