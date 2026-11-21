package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.configuration.UpdatePaymentGatewayConfig;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePaymentGatewayConfigRequest {

    @Pattern(
            regexp = "^sk_(test|live)_.+$",
            message = "Stripe Secret Key không hợp lệ"
    )
    private String secretKey;

    @Pattern(
            regexp = "^whsec_.+$",
            message = "Stripe Webhook Secret không hợp lệ"
    )
    private String webhookSecret;

    @Pattern(
            regexp = "^(ACTIVE|INACTIVE)$",
            message = "Status chỉ nhận ACTIVE hoặc INACTIVE"
    )
    private String status;
}