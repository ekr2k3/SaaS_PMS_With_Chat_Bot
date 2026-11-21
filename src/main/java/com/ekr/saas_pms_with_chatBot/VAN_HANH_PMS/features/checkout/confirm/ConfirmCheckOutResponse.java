package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.confirm;

import java.math.BigDecimal;
import java.util.List;

public record ConfirmCheckOutResponse(

        Long reservationId,

        String reservationCode,

        String reservationStatus,

        String currency,

        BigDecimal totalCharge,

        BigDecimal netPaidAmount,

        BigDecimal balanceDue,

        List<CheckedOutRoom> rooms

) {

    public record CheckedOutRoom(

            Long roomId,

            String roomNumber,

            String previousStatus,

            String currentStatus

    ) {
    }
}
