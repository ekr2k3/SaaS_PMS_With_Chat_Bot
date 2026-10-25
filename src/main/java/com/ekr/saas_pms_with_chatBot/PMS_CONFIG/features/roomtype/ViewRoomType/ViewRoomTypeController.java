
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomtype.ViewRoomType;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(
        "/api/properties/{propertyId}/room-types/{roomTypeId}"
)
@PreAuthorize("hasRole('OWNER')")
public class ViewRoomTypeController {

    private final ViewRoomTypeService viewRoomTypeService;

    public ViewRoomTypeController(
            ViewRoomTypeService viewRoomTypeService
    ) {
        this.viewRoomTypeService = viewRoomTypeService;
    }

    /**
     * GET /api/properties/{propertyId}/room-types/{roomTypeId}
     *
     * Xem chi tiết một Room Type thuộc Property được chọn.
     */
    @GetMapping
    public ViewRoomTypeResponse getRoomType(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @PathVariable Long roomTypeId
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return viewRoomTypeService.getRoomType(
                principal.getUsername(),
                propertyId,
                roomTypeId
        );
    }
}