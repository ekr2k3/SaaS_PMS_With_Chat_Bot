package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.searchreservation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/properties/{propertyId}/check-in/reservations")
public class SearchReservationForCheckInController {

    private final SearchReservationForCheckInService searchService;

    public SearchReservationForCheckInController(
            SearchReservationForCheckInService searchService
    ) {
        this.searchService = searchService;
    }

    @GetMapping("/{reservationCode}")
    public ResponseEntity<SearchReservationForCheckInResponse> search(

            @PathVariable Long propertyId,

            @PathVariable String reservationCode,

            @RequestParam(required = false) String email,

            @RequestParam(required = false) String phone

    ) {

        SearchReservationForCheckInResponse response =
                searchService.search(
                        propertyId,
                        reservationCode,
                        email,
                        phone
                );

        return ResponseEntity.ok(response);
    }
}
