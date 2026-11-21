package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationRefundStatusEmailListener {
    private final ReservationRefundEmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRefundStatusChanged(RefundStatusChangedEvent event) {
        if (event.status() != ReservationPaymentStatus.SUCCEEDED
                && event.status() != ReservationPaymentStatus.FAILED) {
            return;
        }
        try {
            emailService.sendFinalRefundStatus(event.refundPaymentId(), event.status());
        } catch (Exception exception) {
            log.error("Không thể gửi email trạng thái Refund Payment ID {}.",
                    event.refundPaymentId(), exception);
        }
    }
}
