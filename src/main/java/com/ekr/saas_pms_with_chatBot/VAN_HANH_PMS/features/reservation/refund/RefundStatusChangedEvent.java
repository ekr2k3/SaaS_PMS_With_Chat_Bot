package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;

public record RefundStatusChangedEvent(
        Long refundPaymentId,
        ReservationPaymentStatus status
) {}
