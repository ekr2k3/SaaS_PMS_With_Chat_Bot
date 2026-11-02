package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.usermanagement.DeactivateUser;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.User;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.UserRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeactivateUserService {

    private static final String OWNER_ROLE = "OWNER";

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public DeactivateUserService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public DeactivateUserResponse execute(
            String ownerEmail,
            Long propertyId,
            Long userId
    ) {
        // 1. Xác minh Account Owner.
        Account account = accountRepository.findByEmail(ownerEmail);

        if (account == null
                || account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tai khoan Owner khong hop le hoac da bi vo hieu hoa"
            );
        }

        // 2. Xác định Tenant thuộc Account.
        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tai khoan chua duoc lien ket voi Tenant"
                ));

        if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tenant hien khong hoat dong"
            );
        }

        // 3. Xác minh Property thuộc Tenant.
        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Property"
                ));

        // 4. Tìm User trong phạm vi Property.
        User user = userRepository
                .findByUserIdAndProperty_PropertyId(
                        userId,
                        property.getPropertyId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay User trong Property"
                ));

        // 5. Không cho Owner tự vô hiệu hóa chính mình.
        boolean updatingOwnAccount =
                user.getEmail() != null
                        && ownerEmail != null
                        && user.getEmail().trim().equalsIgnoreCase(
                                ownerEmail.trim()
                        );

        if (updatingOwnAccount) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Khong the vo hieu hoa tai khoan Owner dang dang nhap"
            );
        }

        // 6. Nếu User đã INACTIVE thì không cập nhật lại.
        if (user.getStatus() == User.UserStatus.INACTIVE) {
            return new DeactivateUserResponse(
                    user.getUserId(),
                    user.getStatus().name(),
                    "User da duoc vo hieu hoa truoc do."
            );
        }

        // 7. Không cho vô hiệu hóa Owner ACTIVE cuối cùng.
        boolean isOwner =
                user.getRole() != null
                        && OWNER_ROLE.equalsIgnoreCase(
                                user.getRole().getName()
                        );

        if (isOwner) {
            long activeOwners =
                    userRepository
                            .countByProperty_PropertyIdAndRole_NameAndStatus(
                                    property.getPropertyId(),
                                    OWNER_ROLE,
                                    User.UserStatus.ACTIVE
                            );

            if (activeOwners <= 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Khong the vo hieu hoa Owner ACTIVE cuoi cung"
                );
            }
        }

        // 8. Vô hiệu hóa mềm, không xóa vật lý.
        user.setStatus(User.UserStatus.INACTIVE);
        User savedUser = userRepository.save(user);

        return new DeactivateUserResponse(
                savedUser.getUserId(),
                savedUser.getStatus().name(),
                "User da duoc vo hieu hoa."
        );
    }
}
