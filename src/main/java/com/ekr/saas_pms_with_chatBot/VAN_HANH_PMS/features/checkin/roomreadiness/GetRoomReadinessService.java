package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.roomreadiness;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Room;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin.CheckInAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GetRoomReadinessService {

    private final RoomRepository roomRepository;
    private final CheckInAccessService accessService;

    public GetRoomReadinessService(
            RoomRepository roomRepository,
            CheckInAccessService accessService
    ) {
        this.roomRepository = roomRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public GetRoomReadinessResponse getReadiness(
            Long propertyId,
            Long roomId
    ) {

        // Chỉ Receptionist có quyền tra cứu trạng thái phòng.
        accessService.requireReceptionist(propertyId, "ROOM_VIEW");

        // Tìm phòng đồng thời xác minh phòng thuộc Property.
        Room room = roomRepository
                .findByRoomIdAndRoomType_Property_PropertyId(
                        roomId,
                        propertyId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Room not found in this Property"
                ));

        String roomStatus = room.getStatus() == null
                ? ""
                : room.getStatus().trim().toUpperCase();

        boolean isReady = "READY".equals(roomStatus);

        return new GetRoomReadinessResponse(
                room.getRoomId(),
                room.getRoomNumber(),
                room.getFloor(),
                roomStatus,
                isReady,
                room.getIsAvailable()
        );
    }
}