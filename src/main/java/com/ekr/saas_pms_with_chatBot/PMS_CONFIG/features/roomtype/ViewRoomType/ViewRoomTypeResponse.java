
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomtype.ViewRoomType;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomType;

import java.util.List;

public record ViewRoomTypeResponse(
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

    public static ViewRoomTypeResponse from(RoomType roomType) {
        List<String> amenityNames = roomType.getAmenities()
                .stream()
                .map(amenity -> amenity.getAmenity())
                .toList();

        return new ViewRoomTypeResponse(
                roomType.getRoomTypeId(),
                roomType.getProperty().getPropertyId(),
                roomType.getName().name(),
                roomType.getDescription(),
                roomType.getCapacity(),
                roomType.getBedType(),
                roomType.getNumberOfBeds(),
                roomType.getNumberOfRooms(),
                roomType.getImage(),
                roomType.getStatus(),
                amenityNames
        );
    }
}