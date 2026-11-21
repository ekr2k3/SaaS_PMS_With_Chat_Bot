package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.cancel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationCancelledEmailListener {
    private final ReservationCancellationEmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationCancelled(ReservationCancelledEvent event) {
        try {
            emailService.sendCancellationConfirmation(event.reservationId(), event.reason());
        } catch (Exception exception) {
            log.error("Không thể gửi email xác nhận hủy Reservation ID {}.",
                    event.reservationId(), exception);
        }
    }
}
