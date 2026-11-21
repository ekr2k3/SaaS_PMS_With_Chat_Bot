package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refundquote;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GetRefundQuoteResponse(
        String reservationCode,
        String reservationStatus,
        LocalDate checkInDate,
        String cancellationFormula,
        Integer freeCancellationDays,
        long daysBeforeCheckIn,
        BigDecimal totalPaidAmount,
        BigDecimal totalRefundedAmount,
        BigDecimal pendingRefundAmount,
        BigDecimal refundableAmount,
        String currency,
        boolean eligibleForRefund
) {}
