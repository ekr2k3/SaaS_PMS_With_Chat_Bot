package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.room.ViewRoom;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/rooms/{roomId}")
@PreAuthorize("hasRole('OWNER')")
public class ViewRoomController {

    private final ViewRoomService viewRoomService;

    public ViewRoomController(ViewRoomService viewRoomService) {
        this.viewRoomService = viewRoomService;
    }

    /**
     * GET /api/properties/{propertyId}/rooms/{roomId}
     *
     * Xem chi tiết Room thuộc Property.
     */
    @GetMapping
    public ViewRoomResponse getRoom(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @PathVariable Long roomId
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return viewRoomService.getRoom(
                principal.getUsername(),
                propertyId,
                roomId
        );
    }
}