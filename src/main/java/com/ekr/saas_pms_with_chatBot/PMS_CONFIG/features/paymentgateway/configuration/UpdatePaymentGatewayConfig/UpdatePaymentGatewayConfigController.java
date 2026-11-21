package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.configuration.UpdatePaymentGatewayConfig;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/payment-gateway")
@PreAuthorize("hasRole('OWNER')")
public class UpdatePaymentGatewayConfigController {

    private final UpdatePaymentGatewayConfigService service;

    public UpdatePaymentGatewayConfigController(
            UpdatePaymentGatewayConfigService service
    ) {
        this.service = service;
    }

    @PutMapping
    public UpdatePaymentGatewayConfigResponse update(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @Valid @RequestBody UpdatePaymentGatewayConfigRequest request
    ) {

        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chưa đăng nhập"
            );
        }

        return service.execute(
                principal.getUsername(),
                propertyId,
                request
        );
    }
}