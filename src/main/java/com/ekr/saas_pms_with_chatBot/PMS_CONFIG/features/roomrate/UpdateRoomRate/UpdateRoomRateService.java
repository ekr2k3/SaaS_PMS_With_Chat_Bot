package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomrate.UpdateRoomRate;

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
public class UpdateRoomRateService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRateRepository roomRateRepository;

    public UpdateRoomRateService(
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
    public UpdateRoomRateResponse updateRoomRate(
            String ownerEmail,
            Long propertyId,
            Long roomRateId,
            UpdateRoomRateRequest request
    ) {
        // 1. Xac thuc Owner
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

        // 2. Lay Tenant cua Owner
        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Owner chua co Tenant"
                ));

        // 3. Kiem tra Property thuoc Tenant cua Owner
        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Property"
                ));

        // 4. Kiem tra RoomRate hien tai thuoc Property tren URL
        RoomRate roomRate = roomRateRepository
                .findByRoomRateIdAndRoomType_Property_PropertyId(
                        roomRateId,
                        property.getPropertyId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay RoomRate trong Property nay"
                ));

        // 5. Lay RoomType duoc chon trong request
        RoomType roomType = roomTypeRepository
                .findByRoomTypeIdAndProperty_PropertyId(
                        request.roomTypeId(),
                        property.getPropertyId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "RoomType khong ton tai trong Property nay"
                ));

        // 6. Khong cho phep trung RoomType + RateName
        boolean duplicate = roomRateRepository
                .existsByRoomType_RoomTypeIdAndRateNameAndRoomRateIdNot(
                        roomType.getRoomTypeId(),
                        request.rateName(),
                        roomRate.getRoomRateId()
                );

        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "RoomType da co RoomRate voi RateName nay"
            );
        }

        // 7. Cap nhat cac truong duoc phep thay doi
        roomRate.setRoomType(roomType);
        roomRate.setRateName(request.rateName());
        roomRate.setPrice(request.price());
        roomRate.setDescription(request.description());

        // Khong thay doi status trong API nay.
        // Transaction se persist thay doi khi ket thuc thanh cong.
        return UpdateRoomRateResponse.from(roomRate);
    }
}
