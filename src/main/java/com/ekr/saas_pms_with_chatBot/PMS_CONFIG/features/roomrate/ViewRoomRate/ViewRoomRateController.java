package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomrate.ViewRoomRate;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/room-rates/{roomRateId}")
@PreAuthorize("hasRole('OWNER')")
public class ViewRoomRateController {

    private final ViewRoomRateService viewRoomRateService;

    public ViewRoomRateController(
            ViewRoomRateService viewRoomRateService
    ) {
        this.viewRoomRateService = viewRoomRateService;
    }

    /**
     * GET /api/properties/{propertyId}/room-rates/{roomRateId}
     *
     * Xem chi tiết một Room Rate thuộc Property.
     */
    @GetMapping
    public ViewRoomRateResponse getRoomRate(
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

        return viewRoomRateService.getRoomRate(
                principal.getUsername(),
                propertyId,
                roomRateId
        );
    }
}
