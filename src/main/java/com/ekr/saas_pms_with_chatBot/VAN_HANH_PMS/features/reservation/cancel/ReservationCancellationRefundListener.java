package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.cancel;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund.CreateRefundRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReservationCancellationRefundListener {
    private static final Logger log=LoggerFactory.getLogger(ReservationCancellationRefundListener.class);
    private final CreateRefundRequestService refundService;
    public ReservationCancellationRefundListener(CreateRefundRequestService refundService){this.refundService=refundService;}
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void onReservationCancelled(ReservationCancelledEvent event){
        if(!event.guestInitiated()) return;
        try {
            refundService.createAutomaticRefundsForReservationId(event.reservationId(), event.reason());
        } catch(RuntimeException ex) {
            // Cancellation is already committed; keep it successful and leave the failure visible in server logs.
            log.error("Automatic refund initiation failed for reservationId={}",event.reservationId(),ex);
        }
    }
}
