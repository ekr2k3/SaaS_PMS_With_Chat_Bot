package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.view;

import java.time.LocalDate;

public record ViewDailyStatisticsRequest(
        LocalDate fromDate,
        LocalDate toDate
) {
}
