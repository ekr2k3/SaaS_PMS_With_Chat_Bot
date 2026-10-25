
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomtype.CreateRoomType;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/room-types")
@PreAuthorize("hasRole('OWNER')")
public class CreateRoomTypeController {

    private final CreateRoomTypeService createRoomTypeService;

    public CreateRoomTypeController(
            CreateRoomTypeService createRoomTypeService
    ) {
        this.createRoomTypeService = createRoomTypeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateRoomTypeResponse createRoomType(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @Valid @RequestBody CreateRoomTypeRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return createRoomTypeService.createRoomType(
                principal.getUsername(),
                propertyId,
                request
        );
    }
}