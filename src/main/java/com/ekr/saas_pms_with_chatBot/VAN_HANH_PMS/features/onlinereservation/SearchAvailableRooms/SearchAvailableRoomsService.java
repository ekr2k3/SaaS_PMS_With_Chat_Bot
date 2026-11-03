package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.onlinereservation.SearchAvailableRooms;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Room;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomRate;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomType;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomRateRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SearchAvailableRoomsService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private static final Set<ReservationStatus> BLOCKING_STATUSES =
            Set.of(
                    ReservationStatus.CONFIRMED,
                    ReservationStatus.CHECKED_IN
            );

    private final PropertyRepository propertyRepository;
    private final RoomRepository roomRepository;
    private final RoomRateRepository roomRateRepository;

    public SearchAvailableRoomsService(
            PropertyRepository propertyRepository,
            RoomRepository roomRepository,
            RoomRateRepository roomRateRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.roomRepository = roomRepository;
        this.roomRateRepository = roomRateRepository;
    }

    @Transactional(readOnly = true)
    public SearchAvailableRoomsResponse search(
            Long propertyId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer guestCount,
            Long roomTypeId
    ) {
        validateSearchCriteria(
                checkInDate,
                checkOutDate,
                guestCount,
                roomTypeId
        );

        // 1. Xác minh Property tồn tại.
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Property"
                ));

        // 2. Kiểm tra điều kiện hoạt động của Tenant và Subscription.
        validatePropertyEligibility(property);

        // 3. Tìm các Room còn khả dụng.
        LocalDateTime now = LocalDateTime.now();

        List<Room> availableRooms = roomRepository.findAvailableRooms(
                propertyId,
                roomTypeId,
                checkInDate,
                checkOutDate,
                now,
                ACTIVE_STATUS,
                ACTIVE_STATUS,
                BLOCKING_STATUSES,
                ReservationStatus.PENDING_PAYMENT
        );

        if (availableRooms.isEmpty()) {
            return emptyResponse(
                    property,
                    checkInDate,
                    checkOutDate,
                    guestCount
            );
        }

        // 4. Nhóm Room theo Room Type.
        Map<Long, List<Room>> roomsByRoomType =
                availableRooms.stream()
                        .collect(Collectors.groupingBy(
                                room -> room.getRoomType().getRoomTypeId(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        // 5. Tính số phòng tối thiểu cần có cho từng Room Type.
        List<Long> qualifiedRoomTypeIds = new ArrayList<>();

        for (Map.Entry<Long, List<Room>> entry
                : roomsByRoomType.entrySet()) {

            RoomType roomType = entry.getValue()
                    .get(0)
                    .getRoomType();

            int minimumRoomsRequired = calculateMinimumRoomsRequired(
                    guestCount,
                    roomType.getCapacity()
            );

            if (entry.getValue().size() >= minimumRoomsRequired) {
                qualifiedRoomTypeIds.add(entry.getKey());
            }
        }

        if (qualifiedRoomTypeIds.isEmpty()) {
            return emptyResponse(
                    property,
                    checkInDate,
                    checkOutDate,
                    guestCount
            );
        }

        // 6. Lấy toàn bộ Room Rate ACTIVE của các Room Type phù hợp.
        List<RoomRate> activeRoomRates =
                roomRateRepository.findAllByRoomTypeIdsAndStatus(
                        qualifiedRoomTypeIds,
                        ACTIVE_STATUS
                );

        Map<Long, List<RoomRate>> ratesByRoomType =
                activeRoomRates.stream()
                        .collect(Collectors.groupingBy(
                                rate -> rate.getRoomType()
                                        .getRoomTypeId(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        // 7. Tạo kết quả theo từng Room Type.
        List<SearchAvailableRoomsResponse.RoomTypeAvailability>
                roomTypeResults = new ArrayList<>();

        for (Long qualifiedRoomTypeId : qualifiedRoomTypeIds) {

            List<Room> rooms = roomsByRoomType.get(qualifiedRoomTypeId);

            RoomType roomType = rooms.get(0).getRoomType();

            List<RoomRate> rates =
                    ratesByRoomType.getOrDefault(
                            qualifiedRoomTypeId,
                            List.of()
                    );

            // Không trả về Room Type không có bảng giá ACTIVE.
            if (rates.isEmpty()) {
                continue;
            }

            int minimumRoomsRequired = calculateMinimumRoomsRequired(
                    guestCount,
                    roomType.getCapacity()
            );

            List<SearchAvailableRoomsResponse.RoomRateItem>
                    rateItems = rates.stream()
                    .map(rate ->
                            new SearchAvailableRoomsResponse.RoomRateItem(
                                    rate.getRoomRateId(),
                                    rate.getRateName().name(),
                                    rate.getPrice(),
                                    rate.getDescription()
                            )
                    )
                    .toList();

            List<SearchAvailableRoomsResponse.AvailableRoomItem>
                    roomItems = rooms.stream()
                    .map(room ->
                            new SearchAvailableRoomsResponse.AvailableRoomItem(
                                    room.getRoomId(),
                                    room.getRoomNumber(),
                                    room.getFloor(),
                                    room.getDescription()
                            )
                    )
                    .toList();

            roomTypeResults.add(
                    new SearchAvailableRoomsResponse.RoomTypeAvailability(
                            roomType.getRoomTypeId(),
                            roomType.getName().name(),
                            roomType.getDescription(),
                            roomType.getCapacity(),
                            roomType.getBedType(),
                            roomType.getNumberOfBeds(),
                            roomType.getImage(),
                            rooms.size(),
                            minimumRoomsRequired,
                            rateItems,
                            roomItems
                    )
            );
        }

        return new SearchAvailableRoomsResponse(
                property.getPropertyId(),
                property.getName(),
                checkInDate,
                checkOutDate,
                guestCount,
                roomTypeResults
        );
    }

    private void validateSearchCriteria(
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer guestCount,
            Long roomTypeId
    ) {
        if (checkInDate == null || checkOutDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Check-in va Check-out date khong duoc de trong"
            );
        }

        if (checkInDate.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Check-in date khong duoc nam trong qua khu"
            );
        }

        if (!checkOutDate.isAfter(checkInDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Check-out date phai sau Check-in date"
            );
        }

        if (guestCount == null || guestCount <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Guest count phai lon hon 0"
            );
        }

        if (roomTypeId != null && roomTypeId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Room Type ID khong hop le"
            );
        }
    }

    private void validatePropertyEligibility(Property property) {
        Tenant tenant = property.getTenant();

        if (tenant == null
                || tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Property khong thuoc Tenant dang hoat dong"
            );
        }

        Subscription subscription = tenant.getSubscription();

        if (subscription == null
                || subscription.getStatus()
                != Subscription.SubscriptionStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Property khong co Subscription dang hoat dong"
            );
        }

        if (subscription.getEndDate() != null
                && subscription.getEndDate().isBefore(LocalDate.now())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Subscription cua Property da het han"
            );
        }
    }

    private int calculateMinimumRoomsRequired(
            Integer guestCount,
            Integer roomCapacity
    ) {
        return (guestCount + roomCapacity - 1) / roomCapacity;
    }

    private SearchAvailableRoomsResponse emptyResponse(
            Property property,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer guestCount
    ) {
        return new SearchAvailableRoomsResponse(
                property.getPropertyId(),
                property.getName(),
                checkInDate,
                checkOutDate,
                guestCount,
                List.of()
        );
    }

    /**
     * Dùng chung truy vấn availability hiện có cho các backend Tool.
     * Không trả Room ID ra ngoài; chỉ trả số lượng phòng còn khả dụng.
     */
    @Transactional(readOnly = true)
    public int countAvailableRooms(
            Long propertyId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Long roomTypeId
    ) {
        validateSearchCriteria(checkInDate, checkOutDate, 1, roomTypeId);

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Khong tim thay Property"));

        validatePropertyEligibility(property);

        return roomRepository.findAvailableRooms(
                propertyId,
                roomTypeId,
                checkInDate,
                checkOutDate,
                LocalDateTime.now(),
                ACTIVE_STATUS,
                ACTIVE_STATUS,
                BLOCKING_STATUSES,
                ReservationStatus.PENDING_PAYMENT
        ).size();
    }
}
