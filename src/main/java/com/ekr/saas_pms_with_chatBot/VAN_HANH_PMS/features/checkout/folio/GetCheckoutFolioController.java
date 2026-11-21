package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.folio;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/properties/{propertyId}/check-out/reservations"
)
public class GetCheckoutFolioController {

    private final GetCheckoutFolioService service;

    public GetCheckoutFolioController(
            GetCheckoutFolioService service
    ) {
        this.service = service;
    }

    @GetMapping("/{reservationCode}/folio")
    public ResponseEntity<GetCheckoutFolioResponse> getFolio(

            @PathVariable Long propertyId,

            @PathVariable String reservationCode

    ) {

        GetCheckoutFolioResponse response =
                service.getFolio(
                        propertyId,
                        reservationCode
                );

        return ResponseEntity.ok(response);
    }
}
