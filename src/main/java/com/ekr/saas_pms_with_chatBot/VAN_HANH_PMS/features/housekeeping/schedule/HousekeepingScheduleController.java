package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.housekeeping.schedule;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/properties/{propertyId}/housekeeping/schedule")
public class HousekeepingScheduleController {

    private final HousekeepingScheduleService scheduleService;

    public HousekeepingScheduleController(
            HousekeepingScheduleService scheduleService
    ) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public ResponseEntity<HousekeepingScheduleResponse> getSchedule(
            @PathVariable Long propertyId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {

        return ResponseEntity.ok(
                scheduleService.getSchedule(propertyId, date)
        );
    }
}