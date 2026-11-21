package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.confirm;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/properties/{propertyId}/check-out/reservations"
)
public class ConfirmCheckOutController {

    private final ConfirmCheckOutService service;

    public ConfirmCheckOutController(
            ConfirmCheckOutService service
    ) {
        this.service = service;
    }

    @PostMapping("/{reservationCode}/confirm")
    public ResponseEntity<ConfirmCheckOutResponse> confirmCheckOut(

            @PathVariable Long propertyId,

            @PathVariable String reservationCode

    ) {

        ConfirmCheckOutResponse response =
                service.confirmCheckOut(
                        propertyId,
                        reservationCode
                );

        return ResponseEntity.ok(response);
    }
}
