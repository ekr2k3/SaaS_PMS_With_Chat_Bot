package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomtype.DeleteRoomType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomType;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomTypeRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteRoomTypeService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Transactional
    public void deleteRoomType(
            String ownerEmail,
            Long propertyId,
            Long roomTypeId) {

        // 1. Tìm Account theo email của Owner đăng nhập
        Account account = accountRepository.findByEmail(ownerEmail);
        if(account == null){
            new IllegalArgumentException("Account not found");
        }



        // 2. Kiểm tra trạng thái Account
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }

        // 3. Tìm Tenant thuộc Account
        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() ->
                        new IllegalArgumentException("Tenant not found"));

        // 4. Kiểm tra trạng thái Tenant
        if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
            throw new IllegalStateException("Tenant is not active");
        }

        // 5. Tìm Property thuộc Tenant của Owner
        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Property not found"));

        // 6. Tìm Room Type thuộc đúng Property
        RoomType roomType = roomTypeRepository
                .findByRoomTypeIdAndProperty_PropertyId(
                        roomTypeId,
                        property.getPropertyId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Room Type not found"));

        // 7. Xóa cứng Room Type
        // RoomType đã khai báo cascade = ALL và orphanRemoval = true
        // cho danh sách RoomTypeAmenity.
        roomTypeRepository.delete(roomType);

        // Đẩy thao tác xóa xuống database trong transaction hiện tại.
        roomTypeRepository.flush();
    }
}