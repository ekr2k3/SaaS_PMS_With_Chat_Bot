package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.catalog;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-gateways")
@PreAuthorize("hasRole('OWNER')")
public class ViewPaymentGatewayController {

    private final ViewPaymentGatewayService service;

    public ViewPaymentGatewayController(
            ViewPaymentGatewayService service
    ) {
        this.service = service;
    }

    @GetMapping
    public List<PaymentGatewayCatalogResponse> getCatalog() {
        return service.execute();
    }
}