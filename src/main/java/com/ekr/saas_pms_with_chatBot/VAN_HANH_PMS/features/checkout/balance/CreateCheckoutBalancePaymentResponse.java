package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.balance;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType;

import java.math.BigDecimal;

public record CreateCheckoutBalancePaymentResponse(

        Long reservationPaymentId,

        String reservationCode,

        ReservationPaymentType paymentType,

        BigDecimal amount,

        String currency,

        ReservationPaymentStatus status,

        boolean reusedExistingAttempt

) {
}
