
package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Controller;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.AdminSubscriptionPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/subscriptions")
public class AdminSubscriptionController {

    private final AdminSubscriptionPaymentService paymentService;

    public AdminSubscriptionController(
            AdminSubscriptionPaymentService paymentService
    ) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{subscriptionId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> approveSubscription(
            @PathVariable Long subscriptionId
    ) {
        paymentService.approveAndCreatePayment(subscriptionId);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Subscription approved. "
                                + "Payment link has been sent to Owner."
                )
        );
    }

    @PostMapping("/{subscriptionId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> rejectSubscription(
            @PathVariable Long subscriptionId
    ) {
        paymentService.rejectSubscription(subscriptionId);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Đã từ chối yêu cầu Subscription. "
                                + "Hệ thống đã xử lý gửi email thông báo."
                )
        );
    }

    
}