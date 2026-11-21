package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.cancel;

import jakarta.validation.constraints.Size;

public record CancelReservationRequest(
        String email,
        String phone,
        @Size(max=500) String reason
) {}
