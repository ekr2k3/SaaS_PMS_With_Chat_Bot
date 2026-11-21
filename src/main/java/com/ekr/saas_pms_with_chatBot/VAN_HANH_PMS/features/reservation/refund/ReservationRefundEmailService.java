package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.MailService;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Guest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationGuest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationPayment;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationGuestRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationRefundEmailService {
    private final ReservationPaymentRepository payments;
    private final ReservationGuestRepository reservationGuests;
    private final MailService mailService;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void sendFinalRefundStatus(Long refundPaymentId, ReservationPaymentStatus expectedStatus) {
        if (expectedStatus != ReservationPaymentStatus.SUCCEEDED
                && expectedStatus != ReservationPaymentStatus.FAILED) {
            return;
        }

        ReservationPayment refund = payments.findById(refundPaymentId).orElse(null);
        if (refund == null || refund.getPaymentType() != ReservationPaymentType.REFUND
                || refund.getStatus() != expectedStatus || refund.getReservation() == null) {
            return;
        }

        Long reservationId = refund.getReservation().getReservationId();
        List<ReservationGuest> guests = reservationGuests.findAllByReservationId(reservationId);
        Guest guest = guests.stream()
                .map(ReservationGuest::getGuest)
                .filter(Objects::nonNull)
                .filter(g -> g.getEmail() != null && !g.getEmail().isBlank())
                .findFirst()
                .orElse(null);
        if (guest == null) {
            log.info("Reservation {} không có Guest email; bỏ qua email Refund.", reservationId);
            return;
        }

        var reservation = refund.getReservation();
        String propertyName = reservation.getProperty() == null
                ? "Khách sạn" : reservation.getProperty().getName();
        mailService.sendReservationRefundStatusEmail(
                guest.getEmail(),
                guest.getFullName(),
                reservation.getReservationCode(),
                propertyName,
                refund.getAmount().toPlainString(),
                refund.getCurrency(),
                expectedStatus.name(),
                refund.getGatewayRefundId(),
                refund.getNote()
        );
        log.info("Đã gửi email Refund {} cho Reservation {} tới {}.",
                expectedStatus, reservation.getReservationCode(), guest.getEmail());
    }
}
