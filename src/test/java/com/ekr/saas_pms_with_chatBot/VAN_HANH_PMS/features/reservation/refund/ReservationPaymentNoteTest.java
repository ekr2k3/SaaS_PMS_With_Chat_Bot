package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationPayment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class ReservationPaymentNoteTest {
    @Test
    void noteShouldDefaultToNullForOrdinaryPayments() {
        ReservationPayment payment = new ReservationPayment();

        assertNull(payment.getNote());
    }
}
