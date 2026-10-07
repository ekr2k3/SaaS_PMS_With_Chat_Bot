package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.MoMo;

import lombok.Data;

@Data
public class MomoPaymentRequest {

    private String partnerCode;

    private String requestId;

    private String orderId;

    private Long amount;

    private String orderInfo;

    private String redirectUrl;

    private String ipnUrl;

    private String requestType;

    private String extraData;

    private String lang;

    private String signature;
}