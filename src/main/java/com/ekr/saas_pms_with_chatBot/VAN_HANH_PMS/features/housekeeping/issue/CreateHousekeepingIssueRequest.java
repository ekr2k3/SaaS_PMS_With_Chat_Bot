package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.housekeeping.issue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateHousekeepingIssueRequest(

        @NotBlank(message = "Issue description is required")
        @Size(
                max = 5000,
                message = "Issue description must not exceed 5000 characters"
        )
        String issue

) {
}