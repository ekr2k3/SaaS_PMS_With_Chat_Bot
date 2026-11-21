package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.housekeeping.schedule;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationRoom;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRoomRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.housekeeping.HousekeepingAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class HousekeepingScheduleService {

    private final ReservationRoomRepository reservationRoomRepository;
    private final HousekeepingAccessService accessService;

    public HousekeepingScheduleService(
            ReservationRoomRepository reservationRoomRepository,
            HousekeepingAccessService accessService
    ) {
        this.reservationRoomRepository = reservationRoomRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public HousekeepingScheduleResponse getSchedule(
            Long propertyId,
            LocalDate date
    ) {

        accessService.requirePropertyAccess(propertyId);

        List<ReservationStatus> statuses = List.of(
                ReservationStatus.CONFIRMED,
                ReservationStatus.CHECKED_IN,
                ReservationStatus.CHECKED_OUT
        );

        List<ReservationRoom> reservationRooms =
                reservationRoomRepository.findHousekeepingSchedule(
                        propertyId,
                        date,
                        statuses
                );

        List<HousekeepingScheduleResponse.WorkItem> items =
                reservationRooms.stream()
                        .map(item -> toWorkItem(item, date))
                        .toList();

        return new HousekeepingScheduleResponse(
                propertyId,
                date,
                items
        );
    }

    private HousekeepingScheduleResponse.WorkItem toWorkItem(
            ReservationRoom reservationRoom,
            LocalDate date
    ) {

        Reservation reservation = reservationRoom.getReservation();

        String taskType;

        if (reservation.getCheckInDate().equals(date)) {
            taskType = "CHECK_IN";
        } else if (reservation.getCheckOutDate().equals(date)) {
            taskType = "CHECK_OUT";
        } else {
            taskType = "STAY_OVER";
        }

        var room = reservationRoom.getRoom();

        return new HousekeepingScheduleResponse.WorkItem(
                reservation.getReservationId(),
                reservation.getReservationCode(),
                room.getRoomId(),
                room.getRoomNumber(),
                room.getFloor(),
                taskType,
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getStatus().name()
        );
    }
}