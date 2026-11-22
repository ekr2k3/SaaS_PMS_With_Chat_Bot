package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.owner;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/owner/daily-statistics")
public class ViewTenantDailyStatisticsController {

    private final ViewTenantDailyStatisticsService service;

    public ViewTenantDailyStatisticsController(
            ViewTenantDailyStatisticsService service
    ) {
        this.service = service;
    }

    /**
     * F36 - Owner tra cứu báo cáo Daily Statistics
     * của các Property thuộc Tenant.
     */
    @GetMapping
    public ResponseEntity<ViewTenantDailyStatisticsResponse> view(
            @RequestParam(required = false)
            Long propertyId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate businessDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        return ResponseEntity.ok(
                service.view(
                        propertyId,
                        businessDate,
                        fromDate,
                        toDate
                )
        );
    }
}
