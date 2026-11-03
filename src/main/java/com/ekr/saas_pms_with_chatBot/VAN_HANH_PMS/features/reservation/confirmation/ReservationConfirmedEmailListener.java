package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.confirmation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationConfirmedEmailListener {

    private final ReservationConfirmationEmailService emailService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleReservationConfirmed(
            ReservationConfirmedEvent event
    ) {

        try {

            emailService.sendConfirmation(
                    event.reservationId()
            );

        } catch (Exception exception) {

            // Không làm thay đổi trạng thái Reservation đã commit.
            log.error(
                    "Không thể gửi email xác nhận Reservation ID {}.",
                    event.reservationId(),
                    exception
            );
        }
    }
}