package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.noshow.confirm;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund.CreateRefundRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationNoShowRefundListener {

    private final CreateRefundRequestService refundService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onReservationNoShowConfirmed(
            ReservationNoShowConfirmedEvent event
    ) {

        try {

            refundService.createNoShowRefundsForReservationId(
                    event.reservationId()
            );

        } catch (Exception exception) {

            log.error(
                    "Cannot initiate No-Show Refund for Reservation ID {}",
                    event.reservationId(),
                    exception
            );
        }
    }
}
