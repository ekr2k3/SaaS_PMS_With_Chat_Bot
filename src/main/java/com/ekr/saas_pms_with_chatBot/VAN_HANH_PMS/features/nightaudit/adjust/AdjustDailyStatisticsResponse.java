package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.adjust;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.view.ViewDailyStatisticsResponse;

public record AdjustDailyStatisticsResponse(
        String message,
        ViewDailyStatisticsResponse dailyStatistics
) {
}
