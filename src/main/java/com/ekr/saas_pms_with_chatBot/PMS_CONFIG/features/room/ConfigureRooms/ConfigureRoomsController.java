package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.room.ConfigureRooms;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/rooms/configuration")
@PreAuthorize("hasRole('OWNER')")
public class ConfigureRoomsController {

    private final ConfigureRoomsService configureRoomsService;

    public ConfigureRoomsController(
            ConfigureRoomsService configureRoomsService
    ) {
        this.configureRoomsService = configureRoomsService;
    }

    /**
     * PUT /api/properties/{propertyId}/rooms/configuration
     *
     * Lưu toàn bộ danh sách Room trong một transaction.
     */
    @PutMapping
    public ConfigureRoomsResponse configureRooms(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @Valid @RequestBody ConfigureRoomsRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return configureRoomsService.configure(
                principal.getUsername(),
                propertyId,
                request
        );
    }
}