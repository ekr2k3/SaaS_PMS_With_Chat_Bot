
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.propertyservice.RegisterPropertyService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/services")
@PreAuthorize("hasRole('OWNER')")
public class RegisterPropertyServiceController {

    private final RegisterPropertyServiceService registerService;

    public RegisterPropertyServiceController(
            RegisterPropertyServiceService registerService
    ) {
        this.registerService = registerService;
    }

    @PostMapping
    public ResponseEntity<RegisterPropertyServiceResponse> register(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @Valid @RequestBody RegisterPropertyServiceRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chưa đăng nhập"
            );
        }

        RegisterPropertyServiceResponse response =
                registerService.register(
                        principal.getUsername(),
                        propertyId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}