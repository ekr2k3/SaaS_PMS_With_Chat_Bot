package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.usermanagement.UpdateUser;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Role;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.User;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoleRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.UserRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class UpdateUserService {

    private static final String OWNER_ROLE = "OWNER";

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UpdateUserService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public UpdateUserResponse execute(
            String ownerEmail,
            Long propertyId,
            Long userId,
            UpdateUserRequest request
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

        // 2. Xác định Tenant.
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

        // 3. Property phải thuộc Tenant của Owner.
        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Property"
                ));

        // 4. User phải thuộc chính Property được yêu cầu.
        User user = userRepository
                .findByUserIdAndProperty_PropertyId(
                        userId,
                        property.getPropertyId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay User trong Property"
                ));

        // 5. Role mới phải tồn tại và đang ACTIVE.
        Role newRole = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Role khong ton tai"
                ));

        if (newRole.getStatus() != Role.RoleStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role moi khong hoat dong"
            );
        }

        String currentRoleName = user.getRole().getName();
        String newRoleName = newRole.getName();

        boolean changingRole =
                !user.getRole().getRoleId().equals(newRole.getRoleId());

        boolean removingOwnerRole =
                OWNER_ROLE.equalsIgnoreCase(currentRoleName)
                        && !OWNER_ROLE.equalsIgnoreCase(newRoleName);

        // 6. Không cho Owner tự hạ quyền của tài khoản mình.
        boolean updatingOwnAccount =
                user.getEmail() != null
                        && ownerEmail != null
                        && user.getEmail().trim().equalsIgnoreCase(
                                ownerEmail.trim()
                        );

        if (updatingOwnAccount && changingRole && removingOwnerRole) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Khong the tu ha Role Owner cua chinh minh"
            );
        }

        // 7. Không cho hạ Role nếu đây là Owner ACTIVE cuối cùng
        // của Property.
        if (changingRole && removingOwnerRole
                && user.getStatus() == User.UserStatus.ACTIVE) {

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
                        "Khong the ha Role cua Owner ACTIVE cuoi cung"
                );
            }
        }

        // 8. Chỉ cập nhật các trường được phép.
        // Email và password không được thay đổi trong use case này.
        user.setFullName(request.fullName().trim());
        user.setPhone(normalizeOptionalText(request.phone()));
        user.setRole(newRole);

        User savedUser = userRepository.save(user);

        return UpdateUserResponse.from(savedUser);
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
