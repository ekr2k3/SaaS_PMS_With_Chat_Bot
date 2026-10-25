
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomtype.UpdateRoomType;

import java.util.List;

public record UpdateRoomTypeResponse(
        Long roomTypeId,
        Long propertyId,
        String name,
        String description,
        Integer capacity,
        String bedType,
        Integer numberOfBeds,
        Integer numberOfRooms,
        String image,
        String status,
        List<String> amenities
) {
}