package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.room.ViewRoomConfiguration;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/rooms/configuration")
@PreAuthorize("hasRole('OWNER')")
public class ViewRoomConfigurationController {

    private final ViewRoomConfigurationService viewRoomConfigurationService;

    public ViewRoomConfigurationController(
            ViewRoomConfigurationService viewRoomConfigurationService
    ) {
        this.viewRoomConfigurationService = viewRoomConfigurationService;
    }

    /**
     * GET /api/properties/{propertyId}/rooms/configuration
     *
     * Lấy toàn bộ dữ liệu cần thiết để hiển thị
     * trang cấu hình Room của Property.
     */
    @GetMapping
    public ViewRoomConfigurationResponse getConfiguration(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return viewRoomConfigurationService.getConfiguration(
                principal.getUsername(),
                propertyId
        );
    }
}
