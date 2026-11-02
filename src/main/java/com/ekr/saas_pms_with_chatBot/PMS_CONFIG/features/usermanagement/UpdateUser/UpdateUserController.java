package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.usermanagement.UpdateUser;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/users")
@PreAuthorize("hasRole('OWNER')")
public class UpdateUserController {

    private final UpdateUserService updateUserService;

    public UpdateUserController(UpdateUserService updateUserService) {
        this.updateUserService = updateUserService;
    }

    // PUT /api/properties/{propertyId}/users/{userId}
    // Cap nhat thong tin va Role cua User.
    @PutMapping("/{userId}")
    public UpdateUserResponse updateUser(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return updateUserService.execute(
                principal.getUsername(),
                propertyId,
                userId,
                request
        );
    }
}
