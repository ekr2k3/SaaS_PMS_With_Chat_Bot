package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.room.ViewRoomConfiguration;

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

import java.util.List;

@Service
public class ViewRoomConfigurationService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;

    public ViewRoomConfigurationService(
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

    /**
     * Lấy dữ liệu cấu hình Room thuộc Property của Owner.
     *
     * Bao gồm:
     * - Danh sách Room Type và số lượng phòng được cấu hình.
     * - Danh sách Room hiện tại.
     */
    @Transactional(readOnly = true)
    public ViewRoomConfigurationResponse getConfiguration(
            String ownerEmail,
            Long propertyId
    ) {
        // 1. Xác minh Account của Owner.
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

        // 4. Lấy Room Type thuộc Property.
        List<RoomType> roomTypes =
                roomTypeRepository
                        .findAllByProperty_PropertyIdOrderByRoomTypeIdAsc(
                                property.getPropertyId()
                        );

        // 5. Lấy Room thuộc Property thông qua Room Type.
        List<Room> rooms =
                roomRepository
                        .findAllByRoomType_Property_PropertyIdOrderByRoomIdAsc(
                                property.getPropertyId()
                        );

        // 6. Ánh xạ kết quả thành Response.
        return ViewRoomConfigurationResponse.from(
                property.getPropertyId(),
                roomTypes,
                rooms
        );
    }
}