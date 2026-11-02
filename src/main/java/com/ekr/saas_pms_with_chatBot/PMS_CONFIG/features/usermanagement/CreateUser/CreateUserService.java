package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.usermanagement.CreateUser;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Role;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.User;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoleRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.UserRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreateUserService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public CreateUserService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            SubscriptionRepository subscriptionRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CreateUserResponse execute(
            String ownerEmail,
            Long propertyId,
            CreateUserRequest request
    ) {
        // 1. Xác minh Account Owner đang đăng nhập.
        Account account = accountRepository.findByEmail(ownerEmail);

        if (account == null
                || account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tai khoan Owner khong hop le hoac da bi vo hieu hoa"
            );
        }

        // 2. Xác định Tenant của Owner.
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

        // 4. Khóa Subscription để kiểm tra maxUsers an toàn
        // khi có nhiều yêu cầu tạo User đồng thời.
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

        if (plan.getStatus()
                != SubscriptionPlan.PlanStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Subscription Plan khong hoat dong"
            );
        }

        long currentUserCount =
                userRepository.countByProperty_Tenant_TenantId(
                        tenant.getTenantId()
                );

        int maxUsers = plan.getMaxUsers();

        if (currentUserCount >= maxUsers) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tenant da dat gioi han "
                            + maxUsers
                            + " User cua Subscription Plan"
            );
        }

        // 5. Chuẩn hóa và kiểm tra email duy nhất toàn hệ thống.
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email da duoc su dung"
            );
        }

        // 6. Role phải tồn tại và đang ACTIVE.
        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Role khong ton tai"
                ));

        if (role.getStatus() != Role.RoleStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role khong hoat dong"
            );
        }

        // 7. Tạo User; chỉ lưu mật khẩu đã hash.
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(normalizeOptionalText(request.phone()));
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        user.setStatus(User.UserStatus.ACTIVE);
        user.setProperty(property);
        user.setRole(role);

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return CreateUserResponse.from(savedUser);
        } catch (DataIntegrityViolationException exception) {
            // Bắt trường hợp hai request cùng vượt qua bước kiểm tra email
            // trước khi một request ghi dữ liệu xuống database.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email da duoc su dung hoac du lieu vi pham rang buoc",
                    exception
            );
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
