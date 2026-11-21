package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.configuration.UpdatePaymentGatewayConfig;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePaymentGatewayConfigResponse {

    private Long gatewayConfigId;

    private Long propertyId;

    private String gatewayName;

    private String status;

    private String message;
}