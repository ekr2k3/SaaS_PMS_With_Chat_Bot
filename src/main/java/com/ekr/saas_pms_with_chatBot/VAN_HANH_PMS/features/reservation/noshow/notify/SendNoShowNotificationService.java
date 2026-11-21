package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.noshow.notify;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.MailService;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Guest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationGuest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationGuestRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.CheckInAccessService;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.noshow.policy.GetStandardCheckInTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SendNoShowNotificationService {

    private static final String REQUIRED_PERMISSION =
            "RESERVATION_VIEW";

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CheckInAccessService accessService;

    private final ReservationRepository reservationRepository;

    private final ReservationGuestRepository reservationGuestRepository;

    private final GetStandardCheckInTimeService checkInTimeService;

    private final MailService mailService;

    @Transactional(readOnly = true)
    public SendNoShowNotificationResponse sendNotification(
            Long propertyId,
            String reservationCode
    ) {

        // 1. Chỉ Receptionist được thực hiện.
        accessService.requireReceptionist(
                propertyId,
                REQUIRED_PERMISSION
        );

        // 2. Tìm Reservation.
        Reservation reservation = reservationRepository
                .findByReservationCode(reservationCode.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reservation not found"
                ));

        // 3. Kiểm tra Reservation thuộc đúng Property.
        if (reservation.getProperty() == null
                || !propertyId.equals(
                        reservation.getProperty().getPropertyId()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reservation not found"
            );
        }

        // 4. Chỉ gửi thông báo cho Reservation CONFIRMED.
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only CONFIRMED reservations can receive No-Show notification"
            );
        }

        // 5. Kiểm tra đã đến thời điểm Check-in tiêu chuẩn.
        var checkInTime =
                checkInTimeService.getCheckInTime(propertyId);

        LocalDateTime expectedCheckIn =
                reservation.getCheckInDate().atTime(checkInTime);

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(expectedCheckIn)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation has not reached the configured check-in time"
            );
        }

        // 6. Lấy Guest có Email hợp lệ.
        List<ReservationGuest> reservationGuests =
                reservationGuestRepository.findAllByReservationId(
                        reservation.getReservationId()
                );

        Guest guest = reservationGuests.stream()
                .map(ReservationGuest::getGuest)
                .filter(Objects::nonNull)
                .filter(g ->
                        g.getEmail() != null
                                && !g.getEmail().isBlank()
                )
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Reservation does not have a valid guest email"
                ));

        // 7. Lấy Email riêng của Property.
        String propertyEmail =
                reservation.getProperty().getEmail();

        if (propertyEmail == null || propertyEmail.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Property email has not been configured"
            );
        }

        String propertyName =
                reservation.getProperty().getName();

        // 8. Gửi Email.
        mailService.sendNoShowCandidateEmail(
                guest.getEmail(),
                guest.getFullName(),
                reservation.getReservationCode(),
                propertyName,
                propertyEmail,
                reservation.getCheckInDate().format(DATE_FORMAT),
                reservation.getCheckOutDate().format(DATE_FORMAT)
        );

        // 9. Chỉ trả kết quả, không ghi Notification xuống DB.
        return new SendNoShowNotificationResponse(
                reservation.getReservationCode(),
                guest.getEmail(),
                propertyEmail,
                LocalDateTime.now(),
                "No-Show notification email sent successfully"
        );
    }
}
