
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomrate.CreateRoomRate;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomRate;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomType;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomRateRepository;
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
public class CreateRoomRateService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRateRepository roomRateRepository;

    public CreateRoomRateService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            RoomTypeRepository roomTypeRepository,
            RoomRateRepository roomRateRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomRateRepository = roomRateRepository;
    }

    @Transactional
    public CreateRoomRateResponse createRoomRate(
            String ownerEmail,
            Long propertyId,
            CreateRoomRateRequest request
    ) {
        // 1. Xác định Account của Owner.
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

        // 2. Xác định Tenant của Owner.
        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Owner chua co Tenant"
                ));

        // 3. Xác minh Property thuộc Tenant của Owner.
        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Property"
                ));

        // 4. Tìm Room Type được yêu cầu.
        RoomType roomType = roomTypeRepository
                .findById(request.roomTypeId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Room Type"
                ));

        // 5. Room Type phải thuộc chính Property đã xác minh.
        if (!roomType.getProperty()
                .getPropertyId()
                .equals(property.getPropertyId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Room Type khong thuoc Property da chon"
            );
        }

        // 6. Không cho phép trùng Room Type và Rate Name.
        boolean duplicate = roomRateRepository
                .existsByRoomType_RoomTypeIdAndRateName(
                        roomType.getRoomTypeId(),
                        request.rateName()
                );

        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Room Type da co Rate Name nay"
            );
        }

        // 7. Tạo Room Rate mới.
        RoomRate roomRate = RoomRate.builder()
                .roomType(roomType)
                .rateName(request.rateName())
                .price(request.price())
                .status("ACTIVE")
                .description(request.description())
                .build();

        RoomRate savedRoomRate = roomRateRepository.save(roomRate);

        // 8. Trả thông tin bản ghi vừa tạo.
        return CreateRoomRateResponse.from(savedRoomRate);
    }
}