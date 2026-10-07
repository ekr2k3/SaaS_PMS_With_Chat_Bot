package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Controller;


import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.REQ.SubscriptionRequestDTO;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
public class SubscriptionController {
    @Autowired
    SubscriptionService subscriptionService;

    @PostMapping("/sendRequest")
    public ResponseEntity<?> sendRequest(@RequestBody SubscriptionRequestDTO req){
        // Lấy Object Authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return subscriptionService.sendRequestSubscription(req, auth);
    }


}
