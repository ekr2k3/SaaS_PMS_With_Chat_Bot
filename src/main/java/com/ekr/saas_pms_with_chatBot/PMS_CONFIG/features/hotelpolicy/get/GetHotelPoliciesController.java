package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.hotelpolicy.get;


import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/hotel-policies")
@PreAuthorize("hasRole('OWNER')")
public class GetHotelPoliciesController {

    private final GetHotelPoliciesService getHotelPoliciesService;

    public GetHotelPoliciesController(
            GetHotelPoliciesService getHotelPoliciesService
    ) {
        this.getHotelPoliciesService = getHotelPoliciesService;
    }

    @GetMapping
    public GetHotelPoliciesResponse getHotelPolicies(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chua dang nhap"
            );
        }

        return getHotelPoliciesService.getHotelPolicies(
                principal.getUsername(),
                propertyId
        );
    }
}
