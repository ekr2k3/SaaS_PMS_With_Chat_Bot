package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.guestfnb.placeorder;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/reservations")
public class PlaceFnbOrderController {

    private final PlaceFnbOrderService service;

    public PlaceFnbOrderController(
            PlaceFnbOrderService service
    ) {
        this.service = service;
    }

    @PostMapping("/{reservationCode}/fnb-orders")
    public ResponseEntity<PlaceFnbOrderResponse> placeOrder(

            @PathVariable String reservationCode,

            @RequestParam(required = false) String email,

            @RequestParam(required = false) String phone,

            @Valid @RequestBody PlaceFnbOrderRequest request

    ) {

        PlaceFnbOrderResponse response =
                service.placeOrder(
                        reservationCode,
                        email,
                        phone,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}