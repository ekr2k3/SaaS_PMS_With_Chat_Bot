package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomtype.DeleteRoomType;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/properties/{propertyId}/room-types")
@RequiredArgsConstructor
public class DeleteRoomTypeController {

    private final DeleteRoomTypeService deleteRoomTypeService;

    @DeleteMapping("/{roomTypeId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteRoomType(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long propertyId,
            @PathVariable Long roomTypeId) {

        deleteRoomTypeService.deleteRoomType(
                principal.getUsername(),
                propertyId,
                roomTypeId
        );

        return ResponseEntity.noContent().build();
    }
}