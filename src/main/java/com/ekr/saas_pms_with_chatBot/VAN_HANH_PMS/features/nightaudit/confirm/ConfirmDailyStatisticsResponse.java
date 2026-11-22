package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.confirm;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.view.ViewDailyStatisticsResponse;

public record ConfirmDailyStatisticsResponse(
        String message,
        ViewDailyStatisticsResponse dailyStatistics
) {
}
