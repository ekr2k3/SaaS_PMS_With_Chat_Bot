package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.guestprofile;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "/api/properties/{propertyId}/check-in/reservations"
)
public class GetCheckInGuestProfileController {

    private final GetCheckInGuestProfileService service;

    public GetCheckInGuestProfileController(
            GetCheckInGuestProfileService service
    ) {
        this.service = service;
    }

    @GetMapping("/{reservationCode}/guests")
    public ResponseEntity<GetCheckInGuestProfileResponse> getGuestProfiles(
            @PathVariable Long propertyId,
            @PathVariable String reservationCode
    ) {
        return ResponseEntity.ok(
                service.getGuestProfiles(
                        propertyId,
                        reservationCode
                )
        );
    }
}