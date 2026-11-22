package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.confirm;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/properties/{propertyId}/daily-statistics")
public class ConfirmDailyStatisticsController {

    private final ConfirmDailyStatisticsService service;

    public ConfirmDailyStatisticsController(
            ConfirmDailyStatisticsService service
    ) {
        this.service = service;
    }

    /**
     * F35 - Manager xác nhận Night Audit.
     */
    @PostMapping("/{businessDate}/confirm")
    public ResponseEntity<ConfirmDailyStatisticsResponse> confirm(
            @PathVariable Long propertyId,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate businessDate
    ) {
        return ResponseEntity.ok(
                service.confirm(
                        propertyId,
                        businessDate
                )
        );
    }
}
