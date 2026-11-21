package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refundquote;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class GetRefundQuoteController {
    private final GetRefundQuoteService service;
    public GetRefundQuoteController(GetRefundQuoteService service) { this.service = service; }

    @GetMapping("/api/public/reservations/{reservationCode}/refund-quote")
    public ResponseEntity<GetRefundQuoteResponse> guestQuote(
            @PathVariable String reservationCode,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        return ResponseEntity.ok(service.getGuestQuote(reservationCode, email, phone));
    }

    @GetMapping("/api/properties/{propertyId}/reservations/{reservationCode}/refund-quote")
    public ResponseEntity<GetRefundQuoteResponse> receptionistQuote(
            @PathVariable Long propertyId,
            @PathVariable String reservationCode,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        return ResponseEntity.ok(service.getReceptionistQuote(propertyId, reservationCode, email, phone));
    }
}
