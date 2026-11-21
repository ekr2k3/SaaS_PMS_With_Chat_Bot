package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRefundRequestRequest(
        @NotNull Long reservationPaymentId,
        @Size(max=500) String reason
) {}
