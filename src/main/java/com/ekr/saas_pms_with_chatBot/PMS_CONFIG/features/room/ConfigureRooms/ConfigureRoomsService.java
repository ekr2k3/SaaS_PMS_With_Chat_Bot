package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.room.ConfigureRooms;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Room;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomType;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomTypeRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ConfigureRoomsService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;

    public ConfigureRoomsService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            RoomTypeRepository roomTypeRepository,
            RoomRepository roomRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public ConfigureRoomsResponse configure(
            String ownerEmail,
            Long propertyId,
            ConfigureRoomsRequest request
    ) {
        // 1. Xác minh Account.
        Account account = accountRepository.findByEmail(ownerEmail);

        if (account == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Khong tim thay Owner"
            );
        }

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tai khoan Owner khong hoat dong"
            );
        }

        // 2. Lấy Tenant thuộc Owner.
        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Owner chua co Tenant"
                ));

        // 3. Xác minh Property thuộc Tenant.
        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Property"
                ));

        if (request == null || request.getRooms() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Danh sach Room khong duoc null"
            );
        }

        List<ConfigureRoomsRequest.RoomItem> requestedRooms =
                request.getRooms();

        // 4. Lấy Room Type hợp lệ thuộc Property.
        List<RoomType> roomTypes =
                roomTypeRepository
                        .findAllByProperty_PropertyIdOrderByRoomTypeIdAsc(
                                property.getPropertyId()
                        );

        Map<Long, RoomType> roomTypeById = new HashMap<>();

        for (RoomType roomType : roomTypes) {
            roomTypeById.put(
                    roomType.getRoomTypeId(),
                    roomType
            );
        }

        // 5. Lấy các Room hiện có trong Property.
        List<Room> existingRooms =
                roomRepository
                        .findAllByRoomType_Property_PropertyIdOrderByRoomIdAsc(
                                property.getPropertyId()
                        );

        Map<Long, Room> existingRoomById = new HashMap<>();

        for (Room room : existingRooms) {
            existingRoomById.put(room.getRoomId(), room);
        }

        // 6. Kiểm tra Request và tính số lượng cuối cùng.
        Set<Long> submittedRoomIds = new HashSet<>();
        Map<Long, Integer> finalCountByRoomType = new HashMap<>();
        List<Room> roomsToSave = new ArrayList<>();

        int created = 0;
        int updated = 0;

        for (ConfigureRoomsRequest.RoomItem item : requestedRooms) {

            if (item == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Danh sach chua phan tu Room khong hop le"
                );
            }

            if (item.getRoomNumber() == null
                    || item.getRoomNumber().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Room number khong duoc de trong"
                );
            }

            if (item.getFloor() == null || item.getFloor() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Floor phai lon hon 0"
                );
            }

            if (item.getStatus() == null
                    || item.getStatus().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Status khong duoc de trong"
                );
            }

            Long roomTypeId = item.getRoomTypeId();

            RoomType roomType = roomTypeById.get(roomTypeId);

            if (roomType == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Room Type khong thuoc Property hoac khong ton tai"
                );
            }

            finalCountByRoomType.merge(roomTypeId, 1, Integer::sum);

            Room room;

            if (item.getRoomId() == null) {
                // Room mới.
                room = new Room();
                created++;
            } else {
                // Mỗi Room ID chỉ được xuất hiện một lần.
                Long roomId = item.getRoomId();

                if (!submittedRoomIds.add(roomId)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Room ID bi trung trong Request"
                    );
                }

                // Chỉ cập nhật Room đã tồn tại trong Property hiện tại.
                room = existingRoomById.get(roomId);

                if (room == null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Room ID khong thuoc Property hien tai"
                    );
                }

                updated++;
            }

            room.setRoomNumber(item.getRoomNumber().trim());
            room.setFloor(item.getFloor());
            room.setStatus(item.getStatus().trim());
            room.setDescription(item.getDescription());
            room.setIsAvailable(
                    item.getIsAvailable() == null
                            ? Boolean.TRUE
                            : item.getIsAvailable()
            );
            room.setRoomType(roomType);

            roomsToSave.add(room);
        }

        // 7. Bắt buộc số lượng cuối cùng khớp từng Room Type.
        for (RoomType roomType : roomTypes) {
            Long roomTypeId = roomType.getRoomTypeId();

            int actualCount = finalCountByRoomType.getOrDefault(
                    roomTypeId,
                    0
            );

            Integer expectedCount = roomType.getNumberOfRooms();

            if (expectedCount == null || actualCount != expectedCount) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "So luong Room khong khop Room Type ID "
                                + roomTypeId
                                + ". Yeu cau: "
                                + expectedCount
                                + ", hien tai: "
                                + actualCount
                );
            }
        }

        // 8. Xác định các Room bị loại khỏi danh sách.
        List<Room> roomsToDelete = existingRooms.stream()
                .filter(room -> !submittedRoomIds.contains(
                        room.getRoomId()
                ))
                .toList();

        /*
         * Các kiểm tra nghiệp vụ đã hoàn thành trước khi ghi.
         * Việc xóa và lưu nằm trong cùng transaction.
         */
        roomRepository.deleteAll(roomsToDelete);
        roomRepository.saveAll(roomsToSave);
        roomRepository.flush();

        return new ConfigureRoomsResponse(
                property.getPropertyId(),
                created,
                updated,
                roomsToDelete.size(),
                "Luu cau hinh Room thanh cong"
        );
    }
}