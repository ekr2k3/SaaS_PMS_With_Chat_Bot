package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.adjust;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/properties/{propertyId}/daily-statistics")
public class AdjustDailyStatisticsController {

    private final AdjustDailyStatisticsService service;

    public AdjustDailyStatisticsController(
            AdjustDailyStatisticsService service
    ) {
        this.service = service;
    }

    /**
     * F34 - Manager điều chỉnh số liệu Night Audit.
     */
    @PatchMapping("/{businessDate}")
    public ResponseEntity<AdjustDailyStatisticsResponse> adjust(
            @PathVariable Long propertyId,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate businessDate,
            @Valid
            @RequestBody
            AdjustDailyStatisticsRequest request
    ) {
        return ResponseEntity.ok(
                service.adjust(
                        propertyId,
                        businessDate,
                        request
                )
        );
    }
}
