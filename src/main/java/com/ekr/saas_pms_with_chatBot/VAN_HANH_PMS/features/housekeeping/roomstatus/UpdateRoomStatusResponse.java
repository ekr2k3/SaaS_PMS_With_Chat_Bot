package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.housekeeping.roomstatus;

public record UpdateRoomStatusResponse(
        Long roomId,
        String roomNumber,
        String previousStatus,
        String currentStatus
) {
}