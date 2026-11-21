package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationPayment;

import java.math.BigDecimal;

public record NoShowRefundAllocation(
        ReservationPayment originalPayment,
        BigDecimal refundAmount
) {
}
