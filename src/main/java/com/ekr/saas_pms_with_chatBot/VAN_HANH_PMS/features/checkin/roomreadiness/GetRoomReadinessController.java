package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.roomreadiness;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties/{propertyId}/check-in/rooms")
public class GetRoomReadinessController {

    private final GetRoomReadinessService service;

    public GetRoomReadinessController(
            GetRoomReadinessService service
    ) {
        this.service = service;
    }

    @GetMapping("/{roomId}/readiness")
    public ResponseEntity<GetRoomReadinessResponse> getReadiness(
            @PathVariable Long propertyId,
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(
                service.getReadiness(propertyId, roomId)
        );
    }
}