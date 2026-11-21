package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.configuration.CreatePaymentGatewayConfig;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentGatewayConfigResponse {

    private Long gatewayConfigId;

    private Long propertyId;

    private String propertyName;

    private String gatewayName;

    private String status;

    private String message;
}