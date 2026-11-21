package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.noshow.tracking;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Guest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationGuest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationGuestRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.CheckInAccessService;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.noshow.policy.GetStandardCheckInTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetNoShowTrackingService {

    private static final String REQUIRED_PERMISSION =
            "RESERVATION_VIEW";

    private final CheckInAccessService accessService;

    private final ReservationRepository reservationRepository;

    private final ReservationGuestRepository reservationGuestRepository;

    private final GetStandardCheckInTimeService checkInTimeService;

    @Transactional(readOnly = true)
    public GetNoShowTrackingResponse getTracking(Long propertyId) {

        // 1. Chỉ RECEPTIONIST có quyền xem danh sách.
        accessService.requireReceptionist(
                propertyId,
                REQUIRED_PERMISSION
        );

        // 2. Lấy giờ Check-in tiêu chuẩn đã cấu hình cho Property.
        LocalTime checkInTime =
                checkInTimeService.getCheckInTime(propertyId);

        LocalDateTime now = LocalDateTime.now();

        // 3. Chỉ lấy Reservation đang CONFIRMED.
        List<Reservation> confirmedReservations =
                reservationRepository
                        .findAllByProperty_PropertyIdAndStatusOrderByCheckInDateAsc(
                                propertyId,
                                ReservationStatus.CONFIRMED
                        );

        // 4. Lọc Reservation đã đến thời điểm Check-in.
        List<GetNoShowTrackingResponse.ReservationItem> items =
                confirmedReservations.stream()
                        .filter(reservation -> {

                            LocalDateTime expectedCheckIn =
                                    reservation.getCheckInDate()
                                            .atTime(checkInTime);

                            return !now.isBefore(expectedCheckIn);
                        })
                        .map(reservation -> toItem(
                                reservation,
                                checkInTime
                        ))
                        .toList();

        return new GetNoShowTrackingResponse(
                propertyId,
                now,
                items
        );
    }

    private GetNoShowTrackingResponse.ReservationItem toItem(
            Reservation reservation,
            LocalTime checkInTime
    ) {

        Guest guest = reservationGuestRepository
                .findAllByReservationId(
                        reservation.getReservationId()
                )
                .stream()
                .map(ReservationGuest::getGuest)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);

        return new GetNoShowTrackingResponse.ReservationItem(
                reservation.getReservationId(),
                reservation.getReservationCode(),
                guest == null ? null : guest.getFullName(),
                guest == null ? null : guest.getEmail(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getCheckInDate().atTime(checkInTime),
                reservation.getStatus()
        );
    }
}
