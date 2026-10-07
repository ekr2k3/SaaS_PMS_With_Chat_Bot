package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Controller;


import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.REQ.SubscriptionRequestDTO;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.RES.PendingSubscriptionResponse;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
public class SubscriptionController {
    @Autowired
    SubscriptionService subscriptionService;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @PostMapping("/sendRequest")
    public ResponseEntity<?> sendRequest(@RequestBody SubscriptionRequestDTO req){
        // Lấy Object Authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return subscriptionService.sendRequestSubscription(req, auth);
    }

    @GetMapping("/subscriptions/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PendingSubscriptionResponse>> getPendingSubscriptions() {

        List<PendingSubscriptionResponse> response =
                subscriptionRepository
                        .findByStatus(Subscription.SubscriptionStatus.PENDING)
                        .stream()
                        .map(subscription -> new PendingSubscriptionResponse(
                                subscription.getSubscriptionId(),
                                subscription.getTenant().getAccount().getEmail(),
                                subscription.getPlan().getName(),
                                subscription.getTenant().getName(),
                                subscription.getTenant().getDescription()
                        ))
                        .toList();

        return ResponseEntity.ok(response);
    }
}
