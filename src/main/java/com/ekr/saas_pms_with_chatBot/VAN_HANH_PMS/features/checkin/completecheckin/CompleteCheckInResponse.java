package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.completecheckin;

import java.time.LocalDate;
import java.util.List;

public record CompleteCheckInResponse(
        Long reservationId,
        String reservationCode,
        String reservationStatus,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        List<CheckedInRoom> rooms
) {

    public record CheckedInRoom(
            Long roomId,
            String roomNumber,
            Integer floor,
            String roomStatus
    ) {
    }
}