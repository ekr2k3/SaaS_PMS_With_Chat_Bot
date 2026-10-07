package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.MoMo;

import lombok.Data;

@Data
public class MomoPaymentResponse {

    private String partnerCode;

    private String requestId;

    private String orderId;

    private Long amount;

    private Long responseTime;

    private String message;

    private Integer resultCode;

    private String payUrl;

    private String deeplink;

    private String qrCodeUrl;
}
