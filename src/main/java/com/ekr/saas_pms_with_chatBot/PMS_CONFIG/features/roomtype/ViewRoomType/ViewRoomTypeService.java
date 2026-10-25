
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomtype.ViewRoomType;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomType;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomTypeRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ViewRoomTypeService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;

    public ViewRoomTypeService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            RoomTypeRepository roomTypeRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional(readOnly = true)
    public ViewRoomTypeResponse getRoomType(
            String ownerEmail,
            Long propertyId,
            Long roomTypeId
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

        // 2. Lấy Tenant thuộc Owner.
        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Owner chua co Tenant"
                ));

        // 3. Xác minh Property thuộc Tenant hiện tại.
        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Property"
                ));

        // 4. Chỉ lấy Room Type thuộc đúng Property.
        RoomType roomType = roomTypeRepository
                .findByRoomTypeIdAndProperty_PropertyId(
                        roomTypeId,
                        property.getPropertyId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Room Type trong Property nay"
                ));

        // 5. Ánh xạ DTO trong transaction để đọc amenities.
        return ViewRoomTypeResponse.from(roomType);
    }
}