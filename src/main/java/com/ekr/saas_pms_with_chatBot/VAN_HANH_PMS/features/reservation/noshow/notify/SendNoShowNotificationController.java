package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.noshow.notify;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/properties/{propertyId}/reservations")
public class SendNoShowNotificationController {

    private final SendNoShowNotificationService notificationService;

    @PostMapping("/{reservationCode}/no-show/notify")
    public ResponseEntity<SendNoShowNotificationResponse> sendNotification(
            @PathVariable Long propertyId,
            @PathVariable String reservationCode
    ) {

        return ResponseEntity.ok(
                notificationService.sendNotification(
                        propertyId,
                        reservationCode
                )
        );
    }
}
