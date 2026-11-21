package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.housekeeping.issue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateHousekeepingIssueResolutionRequest(

        @NotBlank(message = "Resolution is required")
        @Size(
                max = 5000,
                message = "Resolution must not exceed 5000 characters"
        )
        String resolution

) {
}