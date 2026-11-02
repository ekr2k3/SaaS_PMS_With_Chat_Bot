
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.fnbmenu.UpdateFnbMenu;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateFnbMenuRequest(

        @Size(max = 150, message = "Tên món tối đa 150 ký tự")
        String name,

        @Min(value = 0, message = "maxQuantity không được âm")
        Integer maxQuantity,

        @Min(value = 0, message = "actualQuantity không được âm")
        Integer actualQuantity,

        @DecimalMin(
                value = "0.00",
                message = "purchasePrice không được âm"
        )
        BigDecimal purchasePrice,

        @DecimalMin(
                value = "0.00",
                message = "sellingPricePercent không được âm"
        )
        BigDecimal sellingPricePercent

) {
    public boolean isEmpty() {
        return name == null
                && maxQuantity == null
                && actualQuantity == null
                && purchasePrice == null
                && sellingPricePercent == null;
    }
}