package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.housekeeping.schedule;

import java.time.LocalDate;
import java.util.List;

public record HousekeepingScheduleResponse(
        Long propertyId,
        LocalDate date,
        List<WorkItem> items
) {

    public record WorkItem(
            Long reservationId,
            String reservationCode,
            Long roomId,
            String roomNumber,
            Integer floor,
            String taskType,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            String reservationStatus
    ) {
    }
}