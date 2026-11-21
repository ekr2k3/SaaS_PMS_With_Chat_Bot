package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.cancel;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class ReservationCancelledEmailListenerTest {
    private final ReservationCancellationEmailService emailService =
            mock(ReservationCancellationEmailService.class);
    private final ReservationCancelledEmailListener listener =
            new ReservationCancelledEmailListener(emailService);

    @Test
    void shouldSendCancellationConfirmationWithReason() {
        listener.onReservationCancelled(
                new ReservationCancelledEvent(20L, false, "Guest requested cancellation"));

        verify(emailService).sendCancellationConfirmation(
                20L, "Guest requested cancellation");
    }
}
