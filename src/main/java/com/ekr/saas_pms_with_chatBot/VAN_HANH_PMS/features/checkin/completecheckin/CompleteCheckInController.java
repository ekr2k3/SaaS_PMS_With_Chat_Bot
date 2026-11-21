package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.completecheckin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "/api/properties/{propertyId}/check-in/reservations"
)
public class CompleteCheckInController {

    private final CompleteCheckInService service;

    public CompleteCheckInController(
            CompleteCheckInService service
    ) {
        this.service = service;
    }

    @PostMapping("/{reservationCode}/complete")
    public ResponseEntity<CompleteCheckInResponse> completeCheckIn(
            @PathVariable Long propertyId,
            @PathVariable String reservationCode
    ) {

        return ResponseEntity.ok(
                service.completeCheckIn(
                        propertyId,
                        reservationCode
                )
        );
    }
}