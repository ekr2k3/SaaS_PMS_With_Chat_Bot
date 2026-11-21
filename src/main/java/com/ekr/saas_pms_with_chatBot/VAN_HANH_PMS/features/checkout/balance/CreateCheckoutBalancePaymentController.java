package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.balance;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/properties/{propertyId}/check-out/reservations"
)
public class CreateCheckoutBalancePaymentController {

    private final CreateCheckoutBalancePaymentService service;

    public CreateCheckoutBalancePaymentController(
            CreateCheckoutBalancePaymentService service
    ) {
        this.service = service;
    }

    @PostMapping("/{reservationCode}/balance-payment")
    public ResponseEntity<CreateCheckoutBalancePaymentResponse> createPayment(

            @PathVariable Long propertyId,

            @PathVariable String reservationCode

    ) {

        CreateCheckoutBalancePaymentResponse response =
                service.createPayment(
                        propertyId,
                        reservationCode
                );

        HttpStatus status =
                response.reusedExistingAttempt()
                        ? HttpStatus.OK
                        : HttpStatus.CREATED;

        return ResponseEntity
                .status(status)
                .body(response);
    }
}
