package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.noshow.notify;

import java.time.LocalDateTime;

public record SendNoShowNotificationResponse(

        String reservationCode,

        String guestEmail,

        String propertyEmail,

        LocalDateTime sentAt,

        String message

) {
}
