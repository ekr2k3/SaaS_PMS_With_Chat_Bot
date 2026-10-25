
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomtype.UpdateRoomType;

import jakarta.validation.Valid;
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
public class UpdateRoomTypeController {

    private final UpdateRoomTypeService updateRoomTypeService;

    public UpdateRoomTypeController(
            UpdateRoomTypeService updateRoomTypeService
    ) {
        this.updateRoomTypeService = updateRoomTypeService;
    }

    @PutMapping
    public UpdateRoomTypeResponse updateRoomType(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @PathVariable Long roomTypeId,
            @Valid @RequestBody UpdateRoomTypeRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return updateRoomTypeService.updateRoomType(
                principal.getUsername(),
                propertyId,
                roomTypeId,
                request
        );
    }
}