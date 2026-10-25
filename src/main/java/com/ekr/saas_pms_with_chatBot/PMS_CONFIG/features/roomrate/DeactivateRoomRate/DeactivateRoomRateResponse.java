package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomrate.DeactivateRoomRate;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomRate;

import java.math.BigDecimal;

public record DeactivateRoomRateResponse(
        Long roomRateId,
        Long propertyId,
        Long roomTypeId,
        String roomTypeName,
        String rateName,
        BigDecimal price,
        String status,
        String description
) {

    public static DeactivateRoomRateResponse from(RoomRate roomRate) {
        return new DeactivateRoomRateResponse(
                roomRate.getRoomRateId(),
                roomRate.getRoomType().getProperty().getPropertyId(),
                roomRate.getRoomType().getRoomTypeId(),
                roomRate.getRoomType().getName().name(),
                roomRate.getRateName().name(),
                roomRate.getPrice(),
                roomRate.getStatus(),
                roomRate.getDescription()
        );
    }
}
