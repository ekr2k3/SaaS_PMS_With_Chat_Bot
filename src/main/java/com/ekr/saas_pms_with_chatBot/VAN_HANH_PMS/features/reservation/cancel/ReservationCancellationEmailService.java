package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.cancel;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.MailService;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Guest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationGuest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationGuestRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCancellationEmailService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReservationRepository reservations;
    private final ReservationGuestRepository reservationGuests;
    private final MailService mailService;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void sendCancellationConfirmation(Long reservationId, String reason) {
        Reservation reservation = reservations.findById(reservationId).orElse(null);
        if (reservation == null) {
            log.warn("Không tìm thấy Reservation {} để gửi email hủy.", reservationId);
            return;
        }

        List<ReservationGuest> guests = reservationGuests.findAllByReservationId(reservationId);
        Guest guest = guests.stream()
                .map(ReservationGuest::getGuest)
                .filter(Objects::nonNull)
                .filter(g -> g.getEmail() != null && !g.getEmail().isBlank())
                .findFirst()
                .orElse(null);
        if (guest == null) {
            log.info("Reservation {} không có Guest email; bỏ qua email hủy.", reservation.getReservationCode());
            return;
        }

        String propertyName = reservation.getProperty() == null
                ? "Khách sạn" : reservation.getProperty().getName();
        mailService.sendReservationCancellationEmail(
                guest.getEmail(),
                guest.getFullName(),
                reservation.getReservationCode(),
                propertyName,
                reservation.getCheckInDate().format(DATE_FORMAT),
                reservation.getCheckOutDate().format(DATE_FORMAT),
                reason
        );
        log.info("Đã gửi email xác nhận hủy Reservation {} tới {}.",
                reservation.getReservationCode(), guest.getEmail());
    }
}
