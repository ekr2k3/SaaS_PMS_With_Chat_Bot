package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.housekeeping.roomstatus;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/properties/{propertyId}/housekeeping/rooms")
public class UpdateRoomStatusController {

    private final UpdateRoomStatusService updateService;

    public UpdateRoomStatusController(
            UpdateRoomStatusService updateService
    ) {
        this.updateService = updateService;
    }

    @PatchMapping("/{roomId}/status")
    public ResponseEntity<UpdateRoomStatusResponse> updateStatus(
            @PathVariable Long propertyId,
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomStatusRequest request
    ) {

        return ResponseEntity.ok(
                updateService.update(propertyId, roomId, request)
        );
    }
}