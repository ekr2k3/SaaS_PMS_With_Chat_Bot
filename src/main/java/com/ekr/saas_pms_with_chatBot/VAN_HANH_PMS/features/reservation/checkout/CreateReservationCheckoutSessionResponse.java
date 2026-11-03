package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.checkout;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;

import java.math.BigDecimal;

public record CreateReservationCheckoutSessionResponse(

        Long reservationPaymentId,

        String reservationCode,

        BigDecimal amount,

        String currency,

        ReservationPaymentStatus paymentStatus,

        String checkoutSessionId,

        String checkoutUrl,

        boolean reusedExistingSession

) {
}
