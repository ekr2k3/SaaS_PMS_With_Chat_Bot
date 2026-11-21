package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.guestfnb.placeorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record PlaceFnbOrderRequest(

        @NotEmpty(message = "Danh sách món không được để trống")
        List<@Valid Item> items,

        @NotNull(message = "Thời gian phục vụ là bắt buộc")
        LocalDateTime serviceTime

) {

    public record Item(

            @NotNull(message = "Menu ID là bắt buộc")
            Long menuId,

            @NotNull(message = "Số lượng là bắt buộc")
            @Min(value = 1, message = "Số lượng phải lớn hơn 0")
            Integer quantity

    ) {
    }
}