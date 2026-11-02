
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.propertyservice.UpdatePropertyServiceStatus;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.propertyservice.ViewPropertyService.ViewPropertyServiceResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/services")
@PreAuthorize("hasRole('OWNER')")
public class UpdatePropertyServiceStatusController {

    private final UpdatePropertyServiceStatusService updateService;

    public UpdatePropertyServiceStatusController(
            UpdatePropertyServiceStatusService updateService
    ) {
        this.updateService = updateService;
    }

    @PatchMapping("/{propertyServiceId}/status")
    public ViewPropertyServiceResponse updateStatus(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @PathVariable Long propertyServiceId,
            @Valid @RequestBody UpdatePropertyServiceStatusRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chưa đăng nhập"
            );
        }

        var updated = updateService.updateStatus(
                principal.getUsername(),
                propertyId,
                propertyServiceId,
                request
        );

        return ViewPropertyServiceResponse.from(updated);
    }
}