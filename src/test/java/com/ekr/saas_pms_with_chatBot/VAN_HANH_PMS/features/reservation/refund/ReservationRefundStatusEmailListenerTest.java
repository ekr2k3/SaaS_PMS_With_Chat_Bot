package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class ReservationRefundStatusEmailListenerTest {
    private final ReservationRefundEmailService emailService = mock(ReservationRefundEmailService.class);
    private final ReservationRefundStatusEmailListener listener =
            new ReservationRefundStatusEmailListener(emailService);

    @Test
    void shouldSendEmailForSucceededRefund() {
        listener.onRefundStatusChanged(
                new RefundStatusChangedEvent(10L, ReservationPaymentStatus.SUCCEEDED));

        verify(emailService).sendFinalRefundStatus(10L, ReservationPaymentStatus.SUCCEEDED);
    }

    @Test
    void shouldSendEmailForFailedRefund() {
        listener.onRefundStatusChanged(
                new RefundStatusChangedEvent(11L, ReservationPaymentStatus.FAILED));

        verify(emailService).sendFinalRefundStatus(11L, ReservationPaymentStatus.FAILED);
    }

    @Test
    void shouldNotSendEmailWhileRefundIsProcessing() {
        listener.onRefundStatusChanged(
                new RefundStatusChangedEvent(12L, ReservationPaymentStatus.PROCESSING));

        verifyNoInteractions(emailService);
    }
}
