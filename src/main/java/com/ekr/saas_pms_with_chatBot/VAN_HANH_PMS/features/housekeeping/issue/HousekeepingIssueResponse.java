package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.housekeeping.issue;

import java.time.LocalDateTime;

public record HousekeepingIssueResponse(
        Long issueId,
        Long roomId,
        String roomNumber,
        String issue,
        String resolution,
        Long createdBy,
        String createdByName,
        LocalDateTime createdAt
) {
}