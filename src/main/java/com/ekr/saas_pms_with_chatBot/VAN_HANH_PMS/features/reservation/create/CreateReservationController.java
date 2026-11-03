package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.create;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/properties/{propertyId}/reservations")
public class CreateReservationController {

    private final CreateReservationService createReservationService;

    public CreateReservationController(
            CreateReservationService createReservationService
    ) {
        this.createReservationService = createReservationService;
    }

    @PostMapping
    public ResponseEntity<CreateReservationResponse> createReservation(
            @PathVariable Long propertyId,
            @Valid @RequestBody CreateReservationRequest request
    ) {

        CreateReservationResponse response =
                createReservationService.create(
                        propertyId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
