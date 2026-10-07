package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Controller;


import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.SubscriptionPaymentService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/subscriptions")
@RequiredArgsConstructor
public class AdminSubscriptionController {

    private final SubscriptionPaymentService
            subscriptionPaymentService;


    @PostMapping("/{subscriptionId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> approveSubscription(

            @PathVariable Long subscriptionId

    ) {

        subscriptionPaymentService
                .approveSubscription(
                        subscriptionId
                );


        return ResponseEntity.ok(
                "Subscription approved. "
                        + "Payment URL has been sent to Owner."
        );
    }
}
