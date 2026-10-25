
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomrate.CreateRoomRate;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomRate;

import java.math.BigDecimal;

public record CreateRoomRateResponse(
        Long roomRateId,
        Long propertyId,
        Long roomTypeId,
        String roomTypeName,
        String rateName,
        BigDecimal price,
        String status,
        String description
) {

    public static CreateRoomRateResponse from(RoomRate roomRate) {
        return new CreateRoomRateResponse(
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