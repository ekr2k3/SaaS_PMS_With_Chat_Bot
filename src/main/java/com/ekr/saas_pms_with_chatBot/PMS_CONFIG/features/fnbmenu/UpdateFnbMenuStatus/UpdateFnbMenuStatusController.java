
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.fnbmenu.UpdateFnbMenuStatus;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.fnbmenu.FnbMenuResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/fnb-menu")
@PreAuthorize("hasRole('OWNER')")
public class UpdateFnbMenuStatusController {

    private final UpdateFnbMenuStatusService updateStatusService;

    public UpdateFnbMenuStatusController(
            UpdateFnbMenuStatusService updateStatusService
    ) {
        this.updateStatusService = updateStatusService;
    }

    @PatchMapping("/{menuId}/status")
    public FnbMenuResponse updateStatus(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @PathVariable Long menuId,
            @Valid @RequestBody UpdateFnbMenuStatusRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chưa đăng nhập"
            );
        }

        return FnbMenuResponse.from(
                updateStatusService.updateStatus(
                        principal.getUsername(),
                        propertyId,
                        menuId,
                        request
                )
        );
    }
}