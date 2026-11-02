
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.fnbmenu.CreateFnbMenu;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateFnbMenuRequest(

        @NotBlank(message = "Tên món không được để trống")
        @Size(max = 150, message = "Tên món tối đa 150 ký tự")
        String name,

        @NotNull(message = "maxQuantity là bắt buộc")
        @Min(value = 0, message = "maxQuantity không được âm")
        Integer maxQuantity,

        @NotNull(message = "actualQuantity là bắt buộc")
        @Min(value = 0, message = "actualQuantity không được âm")
        Integer actualQuantity,

        @NotNull(message = "purchasePrice là bắt buộc")
        @DecimalMin(
                value = "0.00",
                message = "purchasePrice không được âm"
        )
        BigDecimal purchasePrice,

        @NotNull(message = "sellingPricePercent là bắt buộc")
        @DecimalMin(
                value = "0.00",
                message = "sellingPricePercent không được âm"
        )
        BigDecimal sellingPricePercent

) {
}