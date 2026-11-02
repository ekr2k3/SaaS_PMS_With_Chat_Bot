package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.usermanagement.DeactivateUser;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/users")
@PreAuthorize("hasRole('OWNER')")
public class DeactivateUserController {

    private final DeactivateUserService deactivateUserService;

    public DeactivateUserController(
            DeactivateUserService deactivateUserService
    ) {
        this.deactivateUserService = deactivateUserService;
    }

    // DELETE /api/properties/{propertyId}/users/{userId}
    // Vo hieu hoa User, khong xoa vat ly.
    @DeleteMapping("/{userId}")
    public DeactivateUserResponse deactivateUser(
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

        return deactivateUserService.execute(
                principal.getUsername(),
                propertyId,
                userId
        );
    }
}
