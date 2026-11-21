package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GetRefundStatusResponse(Long refundPaymentId,String reservationCode,BigDecimal amount,
        String currency,String status,String gatewayRefundId,LocalDateTime paidAt) {}
