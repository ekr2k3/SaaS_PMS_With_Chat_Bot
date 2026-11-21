package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.catalog;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PaymentGatewayCatalog;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PaymentGatewayCatalogRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ViewPaymentGatewayService {

    private final PaymentGatewayCatalogRepository repository;

    public ViewPaymentGatewayService(
            PaymentGatewayCatalogRepository repository
    ) {
        this.repository = repository;
    }

    public List<PaymentGatewayCatalogResponse> execute() {

        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentGatewayCatalogResponse toResponse(
            PaymentGatewayCatalog gateway
    ) {
        return PaymentGatewayCatalogResponse.builder()
                .gatewayId(gateway.getGatewayId())
                .name(gateway.getName())
                .description(gateway.getDescription())
                .status(gateway.getStatus())
                .build();
    }
}