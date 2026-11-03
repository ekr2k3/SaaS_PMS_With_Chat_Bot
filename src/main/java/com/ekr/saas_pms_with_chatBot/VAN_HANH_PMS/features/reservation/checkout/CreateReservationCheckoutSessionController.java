package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.checkout;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/reservations")
public class CreateReservationCheckoutSessionController {

    private final CreateReservationCheckoutSessionService service;

    public CreateReservationCheckoutSessionController(
            CreateReservationCheckoutSessionService service
    ) {
        this.service = service;
    }


    @PostMapping(
            "/{reservationCode}/payments/{paymentId}/checkout-session"
    )
    public ResponseEntity<CreateReservationCheckoutSessionResponse>
    createCheckoutSession(

            @PathVariable String reservationCode,

            @PathVariable Long paymentId,

            @RequestParam(required = false) String email,

            @RequestParam(required = false) String phone

    ) {

        CreateReservationCheckoutSessionResponse response =
                service.createCheckoutSession(
                        reservationCode,
                        paymentId,
                        email,
                        phone
                );

        HttpStatus status =
                response.reusedExistingSession()
                        ? HttpStatus.OK
                        : HttpStatus.CREATED;

        return ResponseEntity
                .status(status)
                .body(response);
    }
}
