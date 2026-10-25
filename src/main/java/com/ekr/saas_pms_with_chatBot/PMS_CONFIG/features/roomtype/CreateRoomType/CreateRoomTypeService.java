
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomtype.CreateRoomType;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomType;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomTypeAmenity;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomTypeAmenityRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomTypeRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CreateRoomTypeService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeAmenityRepository roomTypeAmenityRepository;

    public CreateRoomTypeService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            SubscriptionRepository subscriptionRepository,
            PropertyRepository propertyRepository,
            RoomTypeRepository roomTypeRepository,
            RoomTypeAmenityRepository roomTypeAmenityRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.propertyRepository = propertyRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomTypeAmenityRepository = roomTypeAmenityRepository;
    }

    @Transactional
    public CreateRoomTypeResponse createRoomType(
            String ownerEmail,
            Long propertyId,
            CreateRoomTypeRequest request
    ) {
        // 1. Xác minh Account của Owner.
        Account account = accountRepository.findByEmail(ownerEmail);

        if (account == null
                || account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tai khoan khong hop le hoac da bi vo hieu hoa"
            );
        }

        // 2. Xác minh Tenant thuộc Owner.
        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tai khoan chua duoc lien ket voi Tenant"
                ));

        if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tenant hien khong duoc phep tao Room Type"
            );
        }

        // 3. Khóa Subscription để tuần tự hóa các yêu cầu
        // thay đổi số phòng của cùng một Tenant.
        Subscription subscription = subscriptionRepository
                .findByTenantIdForUpdate(tenant.getTenantId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tenant chua co Subscription"
                ));

        if (subscription.getStatus()
                != Subscription.SubscriptionStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Subscription chua ACTIVE"
            );
        }

        SubscriptionPlan plan = subscription.getPlan();

        if (plan.getStatus() != SubscriptionPlan.PlanStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Plan hien khong hoat dong"
            );
        }

        // 4. Xác minh Property thuộc Tenant hiện tại.
        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Property"
                ));

        // 5. Kiểm tra tổng số phòng trên toàn Tenant.
        long currentTotalRooms =
                roomTypeRepository.sumNumberOfRoomsByTenantId(
                        tenant.getTenantId()
                );

        int maxRooms = plan.getMaxRooms();
        int requestedRooms = request.getNumberOfRooms();

        if (currentTotalRooms + requestedRooms > maxRooms) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vuot gioi han so phong cua Plan. "
                            + "Hien tai: " + currentTotalRooms
                            + ", yeu cau them: " + requestedRooms
                            + ", gioi han: " + maxRooms
            );
        }

        // 6. Chuẩn hóa và loại bỏ tiện ích trùng lặp.
        List<String> amenities = normalizeAmenities(
                request.getAmenities()
        );

        // 7. Tạo Room Type.
        RoomType roomType = RoomType.builder()
                .name(request.getName())
                .description(normalizeOptionalText(
                        request.getDescription()
                ))
                .capacity(request.getCapacity())
                .bedType(request.getBedType().trim())
                .numberOfBeds(request.getNumberOfBeds())
                .numberOfRooms(requestedRooms)
                .image(normalizeOptionalText(request.getImage()))
                .status("ACTIVE")
                .property(property)
                .amenities(new ArrayList<>())
                .build();

        RoomType savedRoomType =
                roomTypeRepository.saveAndFlush(roomType);

        // 8. Lưu amenities sau khi Room Type đã có ID.
        List<RoomTypeAmenity> savedAmenities = amenities.stream()
                .map(amenity -> new RoomTypeAmenity(
                        savedRoomType.getRoomTypeId(),
                        amenity,
                        savedRoomType
                ))
                .toList();

        if (!savedAmenities.isEmpty()) {
            roomTypeAmenityRepository.saveAll(savedAmenities);
            roomTypeAmenityRepository.flush();
        }

        // 9. Trả kết quả.
        return new CreateRoomTypeResponse(
                savedRoomType.getRoomTypeId(),
                property.getPropertyId(),
                savedRoomType.getName().name(),
                savedRoomType.getDescription(),
                savedRoomType.getCapacity(),
                savedRoomType.getBedType(),
                savedRoomType.getNumberOfBeds(),
                savedRoomType.getNumberOfRooms(),
                savedRoomType.getImage(),
                savedRoomType.getStatus(),
                amenities
        );
    }

    private List<String> normalizeAmenities(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        Set<String> uniqueAmenities = new LinkedHashSet<>();

        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Ten amenity khong duoc de trong"
                );
            }

            String amenity = value.trim();

            if (amenity.length() > 100) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Ten amenity toi da 100 ky tu"
                );
            }

            uniqueAmenities.add(amenity);
        }

        return List.copyOf(uniqueAmenities);
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}