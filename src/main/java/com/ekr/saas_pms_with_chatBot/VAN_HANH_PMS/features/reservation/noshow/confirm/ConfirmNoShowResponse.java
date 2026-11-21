package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.noshow.confirm;

import java.time.LocalDateTime;

public record ConfirmNoShowResponse(
        String reservationCode,
        String previousStatus,
        String currentStatus,
        LocalDateTime confirmedAt,
        String message
) {
}
