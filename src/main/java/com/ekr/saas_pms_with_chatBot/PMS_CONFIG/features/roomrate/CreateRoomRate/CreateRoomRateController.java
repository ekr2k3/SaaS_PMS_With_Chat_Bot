
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomrate.CreateRoomRate;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/room-rates")
@PreAuthorize("hasRole('OWNER')")
public class CreateRoomRateController {

    private final CreateRoomRateService createRoomRateService;

    public CreateRoomRateController(
            CreateRoomRateService createRoomRateService
    ) {
        this.createRoomRateService = createRoomRateService;
    }

    /**
     * POST /api/properties/{propertyId}/room-rates
     *
     * Tạo một Room Rate cho Room Type thuộc Property.
     */
    @PostMapping
    public ResponseEntity<CreateRoomRateResponse> createRoomRate(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @Valid @RequestBody CreateRoomRateRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        CreateRoomRateResponse response =
                createRoomRateService.createRoomRate(
                        principal.getUsername(),
                        propertyId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}