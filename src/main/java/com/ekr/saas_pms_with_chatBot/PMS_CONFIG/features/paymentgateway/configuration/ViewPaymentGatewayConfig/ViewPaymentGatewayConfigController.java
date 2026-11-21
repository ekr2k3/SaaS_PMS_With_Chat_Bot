package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.configuration.ViewPaymentGatewayConfig;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/properties/{propertyId}/payment-gateway")
@PreAuthorize("hasRole('OWNER')")
public class ViewPaymentGatewayConfigController {

    private final ViewPaymentGatewayConfigService service;

    public ViewPaymentGatewayConfigController(
            ViewPaymentGatewayConfigService service
    ) {
        this.service = service;
    }

    @GetMapping
    public ViewPaymentGatewayConfigResponse getDetail(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId
    ) {

        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Chưa đăng nhập"
            );
        }

        return service.execute(
                principal.getUsername(),
                propertyId
        );
    }
}