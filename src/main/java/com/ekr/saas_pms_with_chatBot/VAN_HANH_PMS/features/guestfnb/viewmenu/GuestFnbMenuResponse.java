package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.guestfnb.viewmenu;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.FnbMenu;

import java.math.BigDecimal;

public record GuestFnbMenuResponse(
        Long menuId,
        String name,
        BigDecimal sellingPrice,
        Integer availableQuantity
) {

    public static GuestFnbMenuResponse from(FnbMenu menu) {
        return new GuestFnbMenuResponse(
                menu.getFnbMenuId(),
                menu.getName(),
                menu.getSellingPrice(),
                menu.getActualQuantity()
        );
    }
}