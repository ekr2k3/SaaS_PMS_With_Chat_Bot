package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.balance;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/properties/{propertyId}/check-out/reservations"
)
public class GetCheckoutBalanceController {

    private final GetCheckoutBalanceService service;

    public GetCheckoutBalanceController(
            GetCheckoutBalanceService service
    ) {
        this.service = service;
    }

    @GetMapping("/{reservationCode}/balance")
    public ResponseEntity<GetCheckoutBalanceResponse> getBalance(

            @PathVariable Long propertyId,

            @PathVariable String reservationCode

    ) {

        GetCheckoutBalanceResponse response =
                service.getBalance(
                        propertyId,
                        reservationCode
                );

        return ResponseEntity.ok(response);
    }
}
