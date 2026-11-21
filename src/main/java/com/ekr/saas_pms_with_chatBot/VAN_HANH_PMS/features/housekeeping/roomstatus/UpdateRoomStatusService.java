package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.housekeeping.roomstatus;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Room;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.housekeeping.HousekeepingAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class UpdateRoomStatusService {

    private static final Map<String, String> NEXT_STATUS = Map.of(
            "DIRTY", "CLEANING",
            "CLEANING", "READY"
    );

    private final RoomRepository roomRepository;
    private final HousekeepingAccessService accessService;

    public UpdateRoomStatusService(
            RoomRepository roomRepository,
            HousekeepingAccessService accessService
    ) {
        this.roomRepository = roomRepository;
        this.accessService = accessService;
    }

    @Transactional
    public UpdateRoomStatusResponse update(
            Long propertyId,
            Long roomId,
            UpdateRoomStatusRequest request
    ) {

        accessService.requirePropertyAccess(propertyId);

        Room room = roomRepository
                .findByRoomIdAndRoomType_Property_PropertyId(
                        roomId,
                        propertyId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Room not found in this Property"
                ));

        String currentStatus = room.getStatus() == null
                ? ""
                : room.getStatus().trim().toUpperCase();

        String targetStatus = request.status().trim().toUpperCase();

        String allowedNextStatus = NEXT_STATUS.get(currentStatus);

        boolean isInitialHousekeepingStatus =
                allowedNextStatus == null
                        && "DIRTY".equals(targetStatus);

        boolean isValidTransition =
                isInitialHousekeepingStatus
                        || targetStatus.equals(allowedNextStatus);

        if (!isValidTransition) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid status transition: "
                            + currentStatus + " -> " + targetStatus
            );
        }

        room.setStatus(targetStatus);

        return new UpdateRoomStatusResponse(
                room.getRoomId(),
                room.getRoomNumber(),
                currentStatus,
                targetStatus
        );
    }
}