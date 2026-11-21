package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.guestprofile;

import java.time.LocalDate;
import java.util.List;

public record GetCheckInGuestProfileResponse(
        Long reservationId,
        String reservationCode,
        Integer guestCount,
        List<GuestProfile> guests
) {

    public record GuestProfile(
            Long guestId,
            String fullName,
            String phone,
            String email,
            String identityNumber,
            String identityType,
            LocalDate dateOfBirth,
            String gender,
            String nationality,
            String address
    ) {
    }
}