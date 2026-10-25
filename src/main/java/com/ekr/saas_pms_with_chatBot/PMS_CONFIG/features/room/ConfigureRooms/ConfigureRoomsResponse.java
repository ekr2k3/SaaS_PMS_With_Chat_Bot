package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.room.ConfigureRooms;

public record ConfigureRoomsResponse(
        Long propertyId,
        int created,
        int updated,
        int deleted,
        String message
) {
}