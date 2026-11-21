package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.noshow.tracking;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GetNoShowTrackingResponse(

        Long propertyId,

        LocalDateTime checkedAt,

        List<ReservationItem> reservations

) {

    public record ReservationItem(

            Long reservationId,

            String reservationCode,

            String guestName,

            String guestEmail,

            LocalDate checkInDate,

            LocalDate checkOutDate,

            LocalDateTime expectedCheckInAt,

            ReservationStatus status

    ) {
    }
}
