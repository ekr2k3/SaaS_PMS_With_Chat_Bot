package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.catalog;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentGatewayCatalogResponse {

    private Long gatewayId;

    private String name;

    private String description;

    private String status;
}