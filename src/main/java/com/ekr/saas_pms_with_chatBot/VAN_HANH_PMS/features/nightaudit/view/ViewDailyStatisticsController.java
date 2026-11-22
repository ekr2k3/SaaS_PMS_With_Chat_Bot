package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.view;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/properties/{propertyId}/daily-statistics")
public class ViewDailyStatisticsController {

    private final ViewDailyStatisticsService service;

    public ViewDailyStatisticsController(
            ViewDailyStatisticsService service
    ) {
        this.service = service;
    }

    /**
     * F33 - Manager tra cứu báo cáo Night Audit.
     *
     * Chỉ xem, không cập nhật dữ liệu.
     */
    @GetMapping
    public ResponseEntity<List<ViewDailyStatisticsResponse>> getDailyStatistics(
            @PathVariable Long propertyId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        ViewDailyStatisticsRequest request =
                new ViewDailyStatisticsRequest(fromDate, toDate);

        return ResponseEntity.ok(
                service.getDailyStatistics(propertyId, request)
        );
    }
}
