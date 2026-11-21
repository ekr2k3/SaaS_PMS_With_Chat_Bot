package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.balance;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;

import java.math.BigDecimal;

public record GetCheckoutBalanceResponse(

        Long reservationId,

        String reservationCode,

        ReservationStatus reservationStatus,

        String currency,

        BigDecimal roomAmount,

        BigDecimal folioTotal,

        BigDecimal totalCharge,

        BigDecimal totalCapturedAmount,

        BigDecimal totalRefundedAmount,

        BigDecimal pendingRefundAmount,

        BigDecimal netPaidAmount,

        BigDecimal balanceDue,

        BigDecimal overpaidAmount

) {
}
