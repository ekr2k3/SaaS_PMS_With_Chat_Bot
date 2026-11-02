
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.propertyservice.ViewPropertyService;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/properties/{propertyId}/services")
@PreAuthorize("hasRole('OWNER')")
public class ViewPropertyServiceController {

    private final ViewPropertyServiceService viewService;

    public ViewPropertyServiceController(
            ViewPropertyServiceService viewService
    ) {
        this.viewService = viewService;
    }

    @GetMapping
    public List<ViewPropertyServiceResponse> getServices(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chưa đăng nhập"
            );
        }

        return viewService.getServices(
                principal.getUsername(),
                propertyId
        );
    }

    @GetMapping("/{propertyServiceId}")
    public ViewPropertyServiceResponse getService(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @PathVariable Long propertyServiceId
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chưa đăng nhập"
            );
        }

        return viewService.getService(
                principal.getUsername(),
                propertyId,
                propertyServiceId
        );
    }
}