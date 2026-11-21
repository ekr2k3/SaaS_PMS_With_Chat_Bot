package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.noshow.tracking;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/properties/{propertyId}/reservations/no-show")
public class GetNoShowTrackingController {

    private final GetNoShowTrackingService trackingService;

    @GetMapping
    public ResponseEntity<GetNoShowTrackingResponse> getTracking(
            @PathVariable Long propertyId
    ) {

        return ResponseEntity.ok(
                trackingService.getTracking(propertyId)
        );
    }
}
