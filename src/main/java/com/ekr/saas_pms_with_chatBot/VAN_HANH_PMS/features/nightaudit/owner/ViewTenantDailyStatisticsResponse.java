package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.owner;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.view.ViewDailyStatisticsResponse;

import java.util.List;

public record ViewTenantDailyStatisticsResponse(
        Long tenantId,
        int totalReports,
        List<ViewDailyStatisticsResponse> dailyStatistics
) {
}
