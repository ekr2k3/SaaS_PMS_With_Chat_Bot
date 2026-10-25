package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomrate.UpdateRoomRate;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/room-rates/{roomRateId}")
@PreAuthorize("hasRole('OWNER')")
public class UpdateRoomRateController {

    private final UpdateRoomRateService updateRoomRateService;

    public UpdateRoomRateController(
            UpdateRoomRateService updateRoomRateService
    ) {
        this.updateRoomRateService = updateRoomRateService;
    }

    @PutMapping
    public UpdateRoomRateResponse updateRoomRate(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @PathVariable Long roomRateId,
            @Valid @RequestBody UpdateRoomRateRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return updateRoomRateService.updateRoomRate(
                principal.getUsername(),
                propertyId,
                roomRateId,
                request
        );
    }
}
