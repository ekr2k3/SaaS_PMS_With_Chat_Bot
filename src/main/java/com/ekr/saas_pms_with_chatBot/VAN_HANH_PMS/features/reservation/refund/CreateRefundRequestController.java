package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/properties/{propertyId}/reservations/{reservationCode}/refunds")
public class CreateRefundRequestController {
    private final CreateRefundRequestService service;
    public CreateRefundRequestController(CreateRefundRequestService service){this.service=service;}
    @PostMapping
    public ResponseEntity<CreateRefundRequestResponse> create(@PathVariable Long propertyId,
            @PathVariable String reservationCode,@Valid @RequestBody CreateRefundRequestRequest request){
        return ResponseEntity.ok(service.createForReceptionist(propertyId,reservationCode,request));
    }
}
