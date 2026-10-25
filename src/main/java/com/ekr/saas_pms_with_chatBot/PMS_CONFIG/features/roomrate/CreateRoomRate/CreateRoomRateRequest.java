
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomrate.CreateRoomRate;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.enums.RateName;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateRoomRateRequest(

        @NotNull(message = "Room Type ID is required")
        Long roomTypeId,

        @NotNull(message = "Rate Name is required")
        RateName rateName,

        @NotNull(message = "Price is required")
        @DecimalMin(
                value = "0.00",
                message = "Price must be greater than or equal to 0"
        )
        @Digits(
                integer = 13,
                fraction = 2,
                message = "Price must have at most 13 integer digits and 2 decimal places"
        )
        BigDecimal price,

        @Size(
                max = 5000,
                message = "Description must not exceed 5000 characters"
        )
        String description
) {
}