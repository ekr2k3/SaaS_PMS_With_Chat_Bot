package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;


import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.MoMo.MomoPaymentResponse;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPayment;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentStatus;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionPaymentRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionPaymentService {

    private final SubscriptionRepository subscriptionRepository;

    private final SubscriptionPaymentRepository
            subscriptionPaymentRepository;

    private final MomoService momoService;

    private final MailService mailService;


    @Transactional
    public void approveSubscription(
            Long subscriptionId
    ) {

        // ==========================================
        // 1. FIND SUBSCRIPTION
        // ==========================================

        Subscription subscription =
                subscriptionRepository
                        .findById(subscriptionId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Subscription not found"
                                )
                        );


        // ==========================================
        // 2. GENERATE ORDER ID
        // ==========================================

        String orderId =
                "SUB_" + UUID.randomUUID();


        // ==========================================
        // 3. GENERATE REQUEST ID
        // ==========================================

        String requestId =
                UUID.randomUUID().toString();


        // ==========================================
        // 4. CREATE SUBSCRIPTION PAYMENT
        // ==========================================

        SubscriptionPayment payment =
                SubscriptionPayment.builder()

                        .orderId(orderId)

                        .requestId(requestId)

                        .amount(
                                subscription
                                        .getPlan()
                                        .getPrice()
                        )

                        .currency("VND")

                        .status(
                                PaymentStatus.PENDING
                        )

                        .paymentMethod("MOMO")

                        .subscription(subscription)

                        .build();


        // ==========================================
        // 5. SAVE PAYMENT
        // ==========================================

        subscriptionPaymentRepository.save(
                payment
        );


        // ==========================================
        // 6. SEND PAYMENT REQUEST TO MOMO
        // ==========================================

        MomoPaymentResponse momoResponse =
                momoService.createPayment(

                        orderId,

                        requestId,

                        payment.getAmount(),

                        "Thanh toan Subscription"
                );


        // ==========================================
        // 7. CHECK MOMO RESPONSE
        // ==========================================

        if (momoResponse == null
                || momoResponse.getResultCode() != 0
                || momoResponse.getPayUrl() == null) {


            payment.setStatus(
                    PaymentStatus.FAILED
            );

            subscriptionPaymentRepository.save(
                    payment
            );


            throw new RuntimeException(
                    "Cannot create MoMo payment"
            );
        }


        // ==========================================
        // 8. GET OWNER EMAIL
        // ==========================================

        String ownerEmail =
                subscription
                        .getTenant()
                        .getAccount()
                        .getEmail();


        // ==========================================
        // 9. SEND PAYMENT URL TO OWNER
        // ==========================================

        mailService.sendPaymentEmail(

                ownerEmail,

                momoResponse.getPayUrl(),

                payment.getAmount()
        );
    }
}
