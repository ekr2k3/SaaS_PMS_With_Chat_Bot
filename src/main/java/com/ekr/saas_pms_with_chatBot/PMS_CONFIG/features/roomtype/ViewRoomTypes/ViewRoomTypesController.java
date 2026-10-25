
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomtype.ViewRoomTypes;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/properties/{propertyId}/room-types")
@PreAuthorize("hasRole('OWNER')")
public class ViewRoomTypesController {

    private final ViewRoomTypesService viewRoomTypesService;

    public ViewRoomTypesController(
            ViewRoomTypesService viewRoomTypesService
    ) {
        this.viewRoomTypesService = viewRoomTypesService;
    }

    /**
     * GET /api/properties/{propertyId}/room-types
     *
     * Lấy danh sách Room Type thuộc Property được chọn.
     */
    @GetMapping
    public List<ViewRoomTypesResponse> getRoomTypes(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return viewRoomTypesService.getRoomTypes(
                principal.getUsername(),
                propertyId
        );
    }
}