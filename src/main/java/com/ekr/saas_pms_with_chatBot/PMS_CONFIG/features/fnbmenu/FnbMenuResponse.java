
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.fnbmenu;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.FnbMenu;

import java.math.BigDecimal;

public record FnbMenuResponse(
        Long fnbMenuId,
        Long propertyId,
        String name,
        Integer maxQuantity,
        Integer actualQuantity,
        BigDecimal purchasePrice,
        BigDecimal sellingPricePercent,
        BigDecimal sellingPrice,
        String status
) {
    public static FnbMenuResponse from(FnbMenu menu) {
        return new FnbMenuResponse(
                menu.getFnbMenuId(),
                menu.getProperty().getPropertyId(),
                menu.getName(),
                menu.getMaxQuantity(),
                menu.getActualQuantity(),
                menu.getPurchasePrice(),
                menu.getSellingPricePercent(),
                menu.getSellingPrice(),
                menu.getStatus()
        );
    }
}