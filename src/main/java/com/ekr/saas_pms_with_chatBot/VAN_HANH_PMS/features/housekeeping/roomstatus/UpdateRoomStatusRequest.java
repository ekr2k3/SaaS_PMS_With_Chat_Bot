package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.housekeeping.roomstatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateRoomStatusRequest(

        @NotBlank(message = "Status is required")
        @Pattern(
                regexp = "DIRTY|CLEANING|READY",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "Status must be DIRTY, CLEANING or READY"
        )
        String status

) {
}