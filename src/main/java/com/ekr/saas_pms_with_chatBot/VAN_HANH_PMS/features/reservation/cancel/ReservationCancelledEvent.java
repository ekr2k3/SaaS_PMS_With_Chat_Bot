package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.cancel;

public record ReservationCancelledEvent(Long reservationId, boolean guestInitiated, String reason) {}
