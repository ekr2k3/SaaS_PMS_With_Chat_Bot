package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.get;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/reservations")
public class GetReservationController {

    private final GetReservationService getReservationService;

    public GetReservationController(
            GetReservationService getReservationService
    ) {
        this.getReservationService = getReservationService;
    }

    @GetMapping("/{reservationCode}")
    public ResponseEntity<GetReservationResponse> getReservation(

            @PathVariable String reservationCode,

            @RequestParam(required = false) String email,

            @RequestParam(required = false) String phone

    ) {

        GetReservationResponse response =
                getReservationService.getReservation(
                        reservationCode,
                        email,
                        phone
                );

        return ResponseEntity.ok(response);
    }
}
