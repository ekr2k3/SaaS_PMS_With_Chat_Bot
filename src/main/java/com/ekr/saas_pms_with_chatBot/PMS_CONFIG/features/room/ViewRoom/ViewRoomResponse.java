package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.room.ViewRoom;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Room;

public record ViewRoomResponse(
        Long propertyId,
        Long roomId,
        String roomNumber,
        Integer floor,
        String status,
        String description,
        Boolean isAvailable,
        Long roomTypeId,
        String roomTypeName
) {

    public static ViewRoomResponse from(
            Long propertyId,
            Room room
    ) {
        return new ViewRoomResponse(
                propertyId,
                room.getRoomId(),
                room.getRoomNumber(),
                room.getFloor(),
                room.getStatus(),
                room.getDescription(),
                room.getIsAvailable(),
                room.getRoomType().getRoomTypeId(),
                room.getRoomType().getName().name()
        );
    }
}