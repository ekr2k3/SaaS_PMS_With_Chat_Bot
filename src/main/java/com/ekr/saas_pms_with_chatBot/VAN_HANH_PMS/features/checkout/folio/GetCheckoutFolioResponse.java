package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.folio;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;

import java.math.BigDecimal;
import java.util.List;

public record GetCheckoutFolioResponse(

        Long reservationId,

        String reservationCode,

        ReservationStatus reservationStatus,

        String currency,

        List<FolioItem> items,

        BigDecimal folioTotal

) {

    public record FolioItem(

            Long folioId,

            String service,

            Integer quantity,

            BigDecimal unitPrice,

            BigDecimal amount,

            String serviceData

    ) {
    }
}
