package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.noshow.confirm;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/properties/{propertyId}/reservations")
public class ConfirmNoShowController {

    private final ConfirmNoShowService confirmNoShowService;

    @PostMapping("/{reservationCode}/no-show/confirm")
    public ResponseEntity<ConfirmNoShowResponse> confirmNoShow(
            @PathVariable Long propertyId,
            @PathVariable String reservationCode
    ) {

        return ResponseEntity.ok(
                confirmNoShowService.confirmNoShow(
                        propertyId,
                        reservationCode
                )
        );
    }
}
