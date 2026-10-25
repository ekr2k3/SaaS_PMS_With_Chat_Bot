package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomrate.DeactivateRoomRate;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/room-rates/{roomRateId}")
@PreAuthorize("hasRole('OWNER')")
public class DeactivateRoomRateController {

    private final DeactivateRoomRateService deactivateRoomRateService;

    public DeactivateRoomRateController(
            DeactivateRoomRateService deactivateRoomRateService
    ) {
        this.deactivateRoomRateService = deactivateRoomRateService;
    }

    @PatchMapping("/deactivate")
    public DeactivateRoomRateResponse deactivateRoomRate(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @PathVariable Long roomRateId
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return deactivateRoomRateService.deactivateRoomRate(
                principal.getUsername(),
                propertyId,
                roomRateId
        );
    }
}
