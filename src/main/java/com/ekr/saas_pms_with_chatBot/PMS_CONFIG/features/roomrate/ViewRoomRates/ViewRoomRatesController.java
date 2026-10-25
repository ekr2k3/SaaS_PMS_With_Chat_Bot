
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomrate.ViewRoomRates;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/properties/{propertyId}/room-rates")
@PreAuthorize("hasRole('OWNER')")
public class ViewRoomRatesController {

    private final ViewRoomRatesService viewRoomRatesService;

    public ViewRoomRatesController(
            ViewRoomRatesService viewRoomRatesService
    ) {
        this.viewRoomRatesService = viewRoomRatesService;
    }

    /**
     * GET /api/properties/{propertyId}/room-rates
     *
     * Lấy danh sách Room Rate thuộc Property được chọn.
     */
    @GetMapping
    public List<ViewRoomRatesResponse> getRoomRates(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return viewRoomRatesService.getRoomRates(
                principal.getUsername(),
                propertyId
        );
    }
}