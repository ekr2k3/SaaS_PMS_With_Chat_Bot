package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.paymentgateway.configuration.CreatePaymentGatewayConfig;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePaymentGatewayConfigRequest {

    @NotBlank(message = "Secret Key không được để trống")
    @Pattern(
            regexp = "^sk_(test|live)_.+$",
            message = "Stripe Secret Key không hợp lệ"
    )
    private String secretKey;

    @NotBlank(message = "Webhook Secret không được để trống")
    @Pattern(
            regexp = "^whsec_.+$",
            message = "Stripe Webhook Secret không hợp lệ"
    )
    private String webhookSecret;
}