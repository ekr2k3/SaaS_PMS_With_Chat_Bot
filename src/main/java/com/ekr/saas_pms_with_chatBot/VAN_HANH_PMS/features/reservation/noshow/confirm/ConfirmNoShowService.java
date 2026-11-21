package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.noshow.confirm;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.CheckInAccessService;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.noshow.policy.GetStandardCheckInTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConfirmNoShowService {

    private static final String REQUIRED_PERMISSION =
            "RESERVATION_UPDATE";

    private final CheckInAccessService accessService;

    private final ReservationRepository reservationRepository;

    private final GetStandardCheckInTimeService checkInTimeService;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ConfirmNoShowResponse confirmNoShow(
            Long propertyId,
            String reservationCode
    ) {

        // 1. Kiểm tra Role, Permission, Property và Tenant.
        accessService.requireReceptionist(
                propertyId,
                REQUIRED_PERMISSION
        );

        // 2. Khóa Reservation để tránh hai request xác nhận đồng thời.
        Reservation reservation = reservationRepository
                .findByReservationCodeForUpdate(
                        reservationCode.trim()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reservation not found"
                ));

        // 3. Kiểm tra Reservation thuộc Property đang thao tác.
        if (reservation.getProperty() == null
                || !propertyId.equals(
                        reservation.getProperty().getPropertyId()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reservation not found"
            );
        }

        // 4. Chỉ CONFIRMED mới được xác nhận No-Show.
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only CONFIRMED reservations can be marked as NO_SHOW"
            );
        }

        // 5. Lấy giờ Check-in từ Policy CHECK_IN_TIME.
        var checkInTime =
                checkInTimeService.getCheckInTime(propertyId);

        LocalDateTime expectedCheckIn =
                reservation.getCheckInDate().atTime(checkInTime);

        LocalDateTime now = LocalDateTime.now();

        // 6. Chưa đến thời điểm Check-in thì không được xác nhận.
        if (now.isBefore(expectedCheckIn)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation has not reached the configured check-in time"
            );
        }

        // 7. Lưu trạng thái cũ để trả về.
        String previousStatus =
                reservation.getStatus().name();

        // 8. Chuyển trạng thái.
        reservation.setStatus(ReservationStatus.NO_SHOW);

        reservationRepository.save(reservation);

        // 9. Publish Event trong transaction.
        // Listener chỉ xử lý sau khi transaction Commit.
        eventPublisher.publishEvent(
                new ReservationNoShowConfirmedEvent(
                        reservation.getReservationId()
                )
        );

        return new ConfirmNoShowResponse(
                reservation.getReservationCode(),
                previousStatus,
                ReservationStatus.NO_SHOW.name(),
                now,
                "Reservation has been marked as NO_SHOW"
        );
    }
}
