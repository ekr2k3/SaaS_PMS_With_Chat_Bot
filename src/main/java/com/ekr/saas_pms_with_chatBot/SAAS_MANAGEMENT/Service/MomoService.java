package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.MoMo.MomoPaymentRequest;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.MoMo.MomoPaymentResponse;
import com.ekr.saas_pms_with_chatBot.config.MomoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class MomoService {

    private final MomoProperties momoProperties;

    private final RestTemplate restTemplate =
            new RestTemplate();

    public MomoPaymentResponse createPayment(
            String orderId,
            String requestId,
            BigDecimal amount,
            String orderInfo
    ) {

        // =========================
        // CREATE REQUEST
        // =========================

        MomoPaymentRequest request =
                new MomoPaymentRequest();

        request.setPartnerCode(
                momoProperties.getPartnerCode().trim()
        );

        request.setRequestId(
                requestId.trim()
        );

        request.setOrderId(
                orderId.trim()
        );

        request.setAmount(
                amount.longValue()
        );

        request.setOrderInfo(
                orderInfo.trim()
        );

        request.setRedirectUrl(
                momoProperties.getRedirectUrl().trim()
        );

        request.setIpnUrl(
                momoProperties.getIpnUrl().trim()
        );

        request.setRequestType(
                "payWithMethod"
        );

        request.setExtraData("");

        request.setLang("vi");


        // =========================
        // CREATE RAW SIGNATURE
        // =========================

        String rawSignature =
                "accessKey="
                        + momoProperties.getAccessKey()

                        + "&amount="
                        + request.getAmount()

                        + "&extraData="
                        + request.getExtraData()

                        + "&ipnUrl="
                        + request.getIpnUrl()

                        + "&orderId="
                        + request.getOrderId()

                        + "&orderInfo="
                        + request.getOrderInfo()

                        + "&partnerCode="
                        + request.getPartnerCode()

                        + "&redirectUrl="
                        + request.getRedirectUrl()

                        + "&requestId="
                        + request.getRequestId()

                        + "&requestType="
                        + request.getRequestType();


        // =========================
        // HMAC SHA256
        // =========================

        String signature =
                hmacSHA256(
                        momoProperties.getSecretKey(),
                        rawSignature
                );

        request.setSignature(signature);


        // =========================
        // DEBUG
        // =========================

        System.out.println(
                "MoMo rawSignature = [" +
                        rawSignature +
                        "]"
        );

        System.out.println(
                "MoMo signature = [" +
                        signature +
                        "]"
        );


        // =========================
        // SEND REQUEST TO MOMO
        // =========================

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        HttpEntity<MomoPaymentRequest> entity =
                new HttpEntity<>(
                        request,
                        headers
                );


        ResponseEntity<MomoPaymentResponse>
                response =
                restTemplate.exchange(
                        momoProperties.getEndpoint(),
                        HttpMethod.POST,
                        entity,
                        MomoPaymentResponse.class
                );


        return response.getBody();
    }


    // =========================
    // HMAC SHA256
    // =========================

    private String hmacSHA256(
            String secretKey,
            String data
    ) {

        try {

            Mac mac =
                    Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKeySpec =
                    new SecretKeySpec(
                            secretKey.getBytes(
                                    StandardCharsets.UTF_8
                            ),
                            "HmacSHA256"
                    );

            mac.init(secretKeySpec);

            byte[] hash =
                    mac.doFinal(
                            data.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat
                    .of()
                    .formatHex(hash);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Cannot generate MoMo signature",
                    e
            );
        }
    }
}
