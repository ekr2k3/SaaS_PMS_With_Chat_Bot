package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.roomreadiness;

public record GetRoomReadinessResponse(
        Long roomId,
        String roomNumber,
        Integer floor,
        String roomStatus,
        boolean isReady,
        Boolean isAvailable
) {
}