package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.searchreservation;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;

import java.time.LocalDate;
import java.util.List;

public record SearchReservationForCheckInResponse(

        Long reservationId,

        String reservationCode,

        ReservationStatus status,

        LocalDate checkInDate,

        LocalDate checkOutDate,

        Integer guestCount,

        GuestDetail guest,

        List<RoomDetail> rooms

) {

    public record GuestDetail(

            Long guestId,

            String fullName,

            String email,

            String phone,

            String identityNumber,

            String identityType,

            LocalDate dateOfBirth,

            String gender,

            String nationality,

            String address

    ) {
    }

    public record RoomDetail(

            Long roomId,

            String roomNumber,

            Integer floor,

            String status

    ) {
    }
}
