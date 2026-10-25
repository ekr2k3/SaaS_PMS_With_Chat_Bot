
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomtype.ViewRoomTypes;

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

import java.util.List;

@Service
public class ViewRoomTypesService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;

    public ViewRoomTypesService(
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

    /**
     * Lấy danh sách Room Type thuộc Property của Owner.
     */
    @Transactional(readOnly = true)
    public List<ViewRoomTypesResponse> getRoomTypes(
            String ownerEmail,
            Long propertyId
    ) {
        // 1. Xác định Account của Owner đang đăng nhập.
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

        // 2. Xác định Tenant thuộc Owner.
        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Owner chua co Tenant"
                ));

        // 3. Xác minh Property thuộc đúng Tenant.
        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Property"
                ));

        // 4. Lấy Room Type của đúng Property.
        // Việc ánh xạ DTO diễn ra trong transaction để đọc amenities.
        List<RoomType> roomTypes =
                roomTypeRepository
                        .findAllByProperty_PropertyIdOrderByRoomTypeIdAsc(
                                property.getPropertyId()
                        );

        return roomTypes.stream()
                .map(ViewRoomTypesResponse::from)
                .toList();
    }
}