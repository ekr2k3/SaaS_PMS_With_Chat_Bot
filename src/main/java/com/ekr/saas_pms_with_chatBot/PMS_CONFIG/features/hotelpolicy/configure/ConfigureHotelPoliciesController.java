package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.hotelpolicy.configure;


import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/hotel-policies")
@PreAuthorize("hasRole('OWNER')")
public class ConfigureHotelPoliciesController {

    private final ConfigureHotelPoliciesService configureHotelPoliciesService;

    public ConfigureHotelPoliciesController(
            ConfigureHotelPoliciesService configureHotelPoliciesService
    ) {
        this.configureHotelPoliciesService = configureHotelPoliciesService;
    }

    @PostMapping
    public ConfigureHotelPoliciesResponse configureHotelPolicies(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @Valid @RequestBody ConfigureHotelPoliciesRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return configureHotelPoliciesService.configure(
                principal.getUsername(),
                propertyId,
                request
        );
    }
}
