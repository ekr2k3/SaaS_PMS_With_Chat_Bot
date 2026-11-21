package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.configuration.ViewPaymentGatewayConfig;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewPaymentGatewayConfigResponse {

    private Long gatewayConfigId;

    private Long propertyId;

    private String propertyName;

    private Long gatewayId;

    private String gatewayName;

    private String status;

    private boolean secretKeyConfigured;

    private boolean webhookSecretConfigured;
}