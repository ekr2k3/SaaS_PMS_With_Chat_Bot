package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund;

import java.math.BigDecimal;

public record CreateRefundRequestResponse(
        Long refundPaymentId,
        Long originalPaymentId,
        String reservationCode,
        BigDecimal amount,
        String currency,
        String status,
        String gatewayRefundId
) {}
