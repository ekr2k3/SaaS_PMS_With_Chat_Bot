package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.get;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GetReservationResponse(

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

        LocalDateTime createdAt,

        LocalDateTime paymentExpiresAt,

        boolean paymentHoldExpired,

        GuestDetail guest,

        List<RoomDetail> rooms,

        List<PaymentDetail> payments

) {

    public record GuestDetail(

            String fullName,

            String email,

            String phone,

            String identityNumber,

            String identityType,

            LocalDate dateOfBirth,

            String gender,

            String nationality,

            String address

    ) {
    }

    public record RoomDetail(

            Long roomId,

            String roomNumber,

            Integer floor,

            String roomType,

            List<NightlyPriceDetail> nightlyPrices,

            BigDecimal totalPrice

    ) {
    }

    public record NightlyPriceDetail(

            LocalDate stayDate,

            Long roomRateId,

            String rateName,

            BigDecimal price

    ) {
    }

    public record PaymentDetail(

            Long reservationPaymentId,

            ReservationPaymentType paymentType,

            BigDecimal amount,

            String currency,

            ReservationPaymentStatus status,

            LocalDateTime paidAt

    ) {
    }
}
