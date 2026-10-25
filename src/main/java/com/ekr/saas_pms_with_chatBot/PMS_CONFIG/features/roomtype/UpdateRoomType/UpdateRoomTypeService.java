
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomtype.UpdateRoomType;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomType;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomTypeAmenity;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
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
public class UpdateRoomTypeService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;

    public UpdateRoomTypeService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            SubscriptionRepository subscriptionRepository,
            PropertyRepository propertyRepository,
            RoomTypeRepository roomTypeRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.propertyRepository = propertyRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional
    public UpdateRoomTypeResponse updateRoomType(
            String ownerEmail,
            Long propertyId,
            Long roomTypeId,
            UpdateRoomTypeRequest request
    ) {
        // 1. Xác minh Account.
        Account account = accountRepository.findByEmail(ownerEmail);

        if (account == null
                || account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tai khoan khong hop le hoac da bi vo hieu hoa"
            );
        }

        // 2. Xác minh Tenant của Owner.
        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tai khoan chua duoc lien ket voi Tenant"
                ));

        if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tenant hien khong duoc phep cap nhat Room Type"
            );
        }

        // 3. Khóa Subscription để tuần tự hóa các thay đổi hạn mức.
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

        // 4. Xác minh Property thuộc Tenant.
        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Property"
                ));

        // 5. Room Type phải thuộc đúng Property.
        RoomType roomType = roomTypeRepository
                .findByRoomTypeIdAndProperty_PropertyId(
                        roomTypeId,
                        property.getPropertyId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Room Type trong Property nay"
                ));

        // 6. Tính tổng phòng sau cập nhật trên toàn Tenant.
        long currentTenantRooms =
                roomTypeRepository.sumNumberOfRoomsByTenantId(
                        tenant.getTenantId()
                );

        long newTenantRooms =
                currentTenantRooms
                        - roomType.getNumberOfRooms()
                        + request.getNumberOfRooms();

        int maxRooms = plan.getMaxRooms();

        if (newTenantRooms > maxRooms) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vuot gioi han so phong cua Plan. "
                            + "Tong hien tai: " + currentTenantRooms
                            + ", tong sau cap nhat: " + newTenantRooms
                            + ", gioi han: " + maxRooms
            );
        }

        // 7. Chuẩn hóa amenities trước khi sửa entity.
        List<String> newAmenities = normalizeAmenities(
                request.getAmenities()
        );

        // 8. Cập nhật thông tin Room Type.
        roomType.setName(request.getName());
        roomType.setDescription(
                normalizeOptionalText(request.getDescription())
        );
        roomType.setCapacity(request.getCapacity());
        roomType.setBedType(request.getBedType().trim());
        roomType.setNumberOfBeds(request.getNumberOfBeds());
        roomType.setNumberOfRooms(request.getNumberOfRooms());
        roomType.setImage(
                normalizeOptionalText(request.getImage())
        );

        // 9. Thay thế toàn bộ amenities cũ bằng danh sách mới.
        // orphanRemoval sẽ xóa các entity amenity bị loại bỏ.
        List<RoomTypeAmenity> currentAmenities =
                roomType.getAmenities();

        currentAmenities.clear();

        // Flush việc xóa trước khi thêm lại để tránh xung đột
        // khóa chính kép (room_type_id, amenity).
        roomTypeRepository.flush();

        for (String amenity : newAmenities) {
            RoomTypeAmenity roomTypeAmenity =
                    new RoomTypeAmenity(
                            roomType.getRoomTypeId(),
                            amenity,
                            roomType
                    );

            currentAmenities.add(roomTypeAmenity);
        }

        // 10. Lưu cập nhật trong transaction hiện tại.
        RoomType savedRoomType =
                roomTypeRepository.saveAndFlush(roomType);

        return new UpdateRoomTypeResponse(
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
                newAmenities
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