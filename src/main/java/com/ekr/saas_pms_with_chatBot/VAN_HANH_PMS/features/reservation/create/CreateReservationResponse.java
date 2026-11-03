package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.create;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CreateReservationResponse(

        Long reservationId,

        String reservationCode,

        ReservationStatus status,

        LocalDate checkInDate,

        LocalDate checkOutDate,

        Integer numberOfNights,

        Integer guestCount,

        BigDecimal totalAmount,

        BigDecimal depositAmount,

        String currency,

        LocalDateTime paymentExpiresAt,

        List<RoomPriceDetail> rooms

) {

    public record RoomPriceDetail(

            Long roomId,

            String roomNumber,

            Long roomRateId,

            String rateName,

            BigDecimal nightlyPrice,

            BigDecimal totalPrice

    ) {
    }
}
