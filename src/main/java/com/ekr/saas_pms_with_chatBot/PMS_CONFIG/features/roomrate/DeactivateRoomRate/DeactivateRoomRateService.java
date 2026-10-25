package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomrate.DeactivateRoomRate;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomRate;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomRateRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeactivateRoomRateService {

    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final RoomRateRepository roomRateRepository;

    public DeactivateRoomRateService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            RoomRateRepository roomRateRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.roomRateRepository = roomRateRepository;
    }

    @Transactional
    public DeactivateRoomRateResponse deactivateRoomRate(
            String ownerEmail,
            Long propertyId,
            Long roomRateId
    ) {
        // 1. Tim Owner theo email dang dang nhap.
        Account account = accountRepository.findByEmail(ownerEmail);

        if (account == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Khong tim thay Owner"
            );
        }

        // 2. Chi cho phep Owner co tai khoan ACTIVE.
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tai khoan Owner khong hoat dong"
            );
        }

        // 3. Tim Tenant cua Owner.
        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Owner chua co Tenant"
                ));

        // 4. Dam bao Property thuoc Tenant cua Owner.
        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Property"
                ));

        // 5. Chi tim RoomRate thuoc Property da xac thuc.
        RoomRate roomRate = roomRateRepository
                .findByRoomRateIdAndRoomType_Property_PropertyId(
                        roomRateId,
                        property.getPropertyId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay RoomRate trong Property nay"
                ));

        // 6. Chi chuyen trang thai tu ACTIVE sang INACTIVE.
        if (!ACTIVE.equals(roomRate.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chi co the ngung RoomRate dang ACTIVE"
            );
        }

        roomRate.setStatus(INACTIVE);

        // Entity dang duoc quan ly trong transaction.
        // JPA se persist thay doi khi transaction commit.
        return DeactivateRoomRateResponse.from(roomRate);
    }
}
