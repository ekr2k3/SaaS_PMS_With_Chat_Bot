package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.usermanagement.ViewUser;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/users")
@PreAuthorize("hasRole('OWNER')")
public class ViewUserController {

    private final ViewUserService viewUserService;

    public ViewUserController(ViewUserService viewUserService) {
        this.viewUserService = viewUserService;
    }

    // GET /api/properties/{propertyId}/users/{userId}
    // Xem chi tiet User thuoc Property cua Owner dang dang nhap.
    @GetMapping("/{userId}")
    public ViewUserResponse viewUser(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @PathVariable Long userId
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return viewUserService.viewUser(
                principal.getUsername(),
                propertyId,
                userId
        );
    }
}
