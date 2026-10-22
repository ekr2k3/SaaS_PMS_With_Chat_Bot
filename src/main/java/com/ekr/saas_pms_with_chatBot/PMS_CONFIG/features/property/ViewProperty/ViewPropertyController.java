package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.property.ViewProperty;


import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/properties")
@PreAuthorize("hasRole('OWNER')")
public class ViewPropertyController {

    private final ViewPropertyService viewPropertyService;

    public ViewPropertyController(
            ViewPropertyService viewPropertyService
    ) {
        this.viewPropertyService = viewPropertyService;
    }

    // API 1: GET /api/properties
    // Xem danh sach Property cua Owner dang dang nhap.
    @GetMapping
    public List<ViewPropertyResponse> getMyProperties(
            @AuthenticationPrincipal UserDetails principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return viewPropertyService.getMyProperties(
                principal.getUsername()
        );
    }

    // API 2: GET /api/properties/{propertyId}
    // Xem chi tiet Property thuoc Tenant cua Owner dang dang nhap.
    @GetMapping("/{propertyId}")
    public ViewPropertyResponse getMyProperty(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return viewPropertyService.getMyProperty(
                principal.getUsername(),
                propertyId
        );
    }
}
