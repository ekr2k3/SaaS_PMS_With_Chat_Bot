package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.room.ViewRoomConfiguration;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Room;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomType;

import java.util.List;

public record ViewRoomConfigurationResponse(
        Long propertyId,
        List<RoomTypeItem> roomTypes,
        List<RoomItem> rooms
) {

    public static ViewRoomConfigurationResponse from(
            Long propertyId,
            List<RoomType> roomTypes,
            List<Room> rooms
    ) {
        List<RoomTypeItem> roomTypeItems = roomTypes.stream()
                .map(RoomTypeItem::from)
                .toList();

        List<RoomItem> roomItems = rooms.stream()
                .map(RoomItem::from)
                .toList();

        return new ViewRoomConfigurationResponse(
                propertyId,
                roomTypeItems,
                roomItems
        );
    }

    /**
     * Thông tin Room Type cần cho trang cấu hình Room.
     */
    public record RoomTypeItem(
            Long roomTypeId,
            String name,
            Integer numberOfRooms
    ) {

        public static RoomTypeItem from(RoomType roomType) {
            return new RoomTypeItem(
                    roomType.getRoomTypeId(),
                    roomType.getName().name(),
                    roomType.getNumberOfRooms()
            );
        }
    }

    /**
     * Thông tin Room hiện có.
     */
    public record RoomItem(
            Long roomId,
            String roomNumber,
            Integer floor,
            String status,
            String description,
            Boolean isAvailable,
            Long roomTypeId
    ) {

        public static RoomItem from(Room room) {
            return new RoomItem(
                    room.getRoomId(),
                    room.getRoomNumber(),
                    room.getFloor(),
                    room.getStatus(),
                    room.getDescription(),
                    room.getIsAvailable(),
                    room.getRoomType().getRoomTypeId()
            );
        }
    }
}