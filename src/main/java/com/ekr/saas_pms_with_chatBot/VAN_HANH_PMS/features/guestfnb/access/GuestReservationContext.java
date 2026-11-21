package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.guestfnb.access;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Guest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;

public record GuestReservationContext(
        Reservation reservation,
        Guest guest
) {
}