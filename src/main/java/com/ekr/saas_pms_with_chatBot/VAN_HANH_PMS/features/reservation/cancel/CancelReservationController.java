package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.cancel;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CancelReservationController {
    private final CancelReservationService service;
    public CancelReservationController(CancelReservationService service){this.service=service;}

    @PostMapping("/api/public/reservations/{reservationCode}/cancel")
    public ResponseEntity<CancelReservationResponse> cancelByGuest(@PathVariable String reservationCode,
            @Valid @RequestBody CancelReservationRequest request){
        return ResponseEntity.ok(service.cancelByGuest(reservationCode,request));
    }

    @PostMapping("/api/properties/{propertyId}/reservations/{reservationCode}/cancel")
    public ResponseEntity<CancelReservationResponse> cancelByReceptionist(@PathVariable Long propertyId,
            @PathVariable String reservationCode,@Valid @RequestBody CancelReservationRequest request){
        return ResponseEntity.ok(service.cancelByReceptionist(propertyId,reservationCode,request));
    }
}
