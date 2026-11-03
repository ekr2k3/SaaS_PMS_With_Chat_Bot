package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.create;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record CreateReservationRequest(

        @NotNull
        LocalDate checkInDate,

        @NotNull
        LocalDate checkOutDate,

        @NotNull
        @Min(1)
        Integer guestCount,

        @NotEmpty
        List<@Valid RoomSelection> rooms,

        @NotNull
        @Valid
        GuestInformation guest

) {

    public record RoomSelection(

            @NotNull
            Long roomId,

            @NotNull
            Long roomRateId

    ) {
    }

    public record GuestInformation(

            @NotBlank
            @Size(max = 150)
            String fullName,

            @Email
            @Size(max = 255)
            String email,

            @Size(max = 20)
            String phone,

            @Size(max = 50)
            String identityNumber,

            @Size(max = 20)
            String identityType,

            LocalDate dateOfBirth,

            @Size(max = 20)
            String gender,

            @Size(max = 100)
            String nationality,

            @Size(max = 255)
            String address

    ) {
    }
}
