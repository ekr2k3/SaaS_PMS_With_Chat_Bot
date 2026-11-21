package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/properties/{propertyId}/refunds")
public class GetRefundStatusController {
    private final GetRefundStatusService service;
    public GetRefundStatusController(GetRefundStatusService service){this.service=service;}
    @GetMapping("/{refundPaymentId}")
    public ResponseEntity<GetRefundStatusResponse> get(@PathVariable Long propertyId,@PathVariable Long refundPaymentId){
        return ResponseEntity.ok(service.get(propertyId,refundPaymentId));
    }
}
