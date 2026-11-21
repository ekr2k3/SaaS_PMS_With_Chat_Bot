package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.guestfnb.placeorder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PlaceFnbOrderResponse(

        String reservationCode,

        Long guestId,

        LocalDateTime serviceTime,

        List<ItemResult> items,

        BigDecimal totalAmount,

        String message

) {

    public record ItemResult(

            Long menuId,

            String menuName,

            Integer quantity,

            BigDecimal unitPrice,

            BigDecimal amount,

            Integer remainingQuantity,

            Long folioId

    ) {
    }
}