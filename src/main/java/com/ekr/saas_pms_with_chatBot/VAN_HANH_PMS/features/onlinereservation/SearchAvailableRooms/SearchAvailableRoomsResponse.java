package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.onlinereservation.SearchAvailableRooms;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SearchAvailableRoomsResponse(
        Long propertyId,
        String propertyName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer guestCount,
        List<RoomTypeAvailability> roomTypes
) {

    public record RoomTypeAvailability(
            Long roomTypeId,
            String name,
            String description,
            Integer capacity,
            String bedType,
            Integer numberOfBeds,
            String image,
            Integer availableRoomCount,
            Integer minimumRoomsRequired,
            List<RoomRateItem> roomRates,
            List<AvailableRoomItem> rooms
    ) {
    }

    public record RoomRateItem(
            Long roomRateId,
            String rateName,
            BigDecimal price,
            String description
    ) {
    }

    public record AvailableRoomItem(
            Long roomId,
            String roomNumber,
            Integer floor,
            String description
    ) {
    }
}
