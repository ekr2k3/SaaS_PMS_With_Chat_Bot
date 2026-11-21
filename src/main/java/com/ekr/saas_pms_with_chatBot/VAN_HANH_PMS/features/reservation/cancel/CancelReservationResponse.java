package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.cancel;

import java.math.BigDecimal;

public record CancelReservationResponse(String reservationCode,String status,BigDecimal refundAmount,String currency,String refundStatus,String message) {}
