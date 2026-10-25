package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomrate.ViewRoomRate;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomRate;

import java.math.BigDecimal;

public record ViewRoomRateResponse(
        Long roomRateId,
        Long propertyId,
        Long roomTypeId,
        String roomTypeName,
        String rateName,
        BigDecimal price,
        String status,
        String description
) {

    public static ViewRoomRateResponse from(RoomRate roomRate) {
        return new ViewRoomRateResponse(
                roomRate.getRoomRateId(),
                roomRate.getRoomType()
                        .getProperty()
                        .getPropertyId(),
                roomRate.getRoomType().getRoomTypeId(),
                roomRate.getRoomType().getName().name(),
                roomRate.getRateName().name(),
                roomRate.getPrice(),
                roomRate.getStatus(),
                roomRate.getDescription()
        );
    }
}
