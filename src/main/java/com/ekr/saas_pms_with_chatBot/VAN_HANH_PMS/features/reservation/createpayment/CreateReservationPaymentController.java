package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.createpayment;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/reservations")
public class CreateReservationPaymentController {

    private final CreateReservationPaymentService service;


    @PostMapping("/{reservationCode}/payments")
    public ResponseEntity<CreateReservationPaymentResponse> createPayment(
            @PathVariable String reservationCode,

            @RequestParam(required = false) String email,

            @RequestParam(required = false) String phone
    ) {

        CreateReservationPaymentResponse response =
                service.createPayment(
                        reservationCode,
                        email,
                        phone
                );

        HttpStatus status = response.reusedExistingAttempt()
                ? HttpStatus.OK
                : HttpStatus.CREATED;

        return ResponseEntity
                .status(status)
                .body(response);
    }
}
