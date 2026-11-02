package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.authentication.PmsEmployeeLogin;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.*;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.*;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.*;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;
import com.ekr.saas_pms_with_chatBot.filter.JwtHelper;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class PmsEmployeeLoginService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtHelper jwtHelper;

    public PmsEmployeeLoginService(
            UserRepository userRepository,
            RolePermissionRepository rolePermissionRepository,
            SubscriptionRepository subscriptionRepository,
            PasswordEncoder passwordEncoder,
            JwtHelper jwtHelper
    ) {
        this.userRepository = userRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtHelper = jwtHelper;
    }

    @Transactional(readOnly = true)
    public PmsEmployeeLoginResponse login(
            PmsEmployeeLoginRequest request
    ) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // 1. Tìm nhân viên theo email.
        User user = userRepository.findByEmail(email)
                .orElseThrow(this::invalidCredentials);

        // 2. Kiểm tra mật khẩu.
        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw invalidCredentials();
        }

        // 3. Kiểm tra trạng thái User.
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw forbidden(
                    "Tai khoan nhan vien da bi vo hieu hoa"
            );
        }

        // 4. Kiểm tra Role.
        if (user.getRole() == null
                || user.getRole().getStatus()
                != Role.RoleStatus.ACTIVE) {

            throw forbidden(
                    "Role cua nhan vien khong hoat dong"
            );
        }

        // Owner sử dụng SaaS Login, không sử dụng Employee Login.
        if ("OWNER".equalsIgnoreCase(
                user.getRole().getName()
        )) {

            throw forbidden(
                    "Tai khoan Owner su dung dang nhap SaaS"
            );
        }

        // 5. Kiểm tra Property và Tenant.
        var property = user.getProperty();

        if (property == null || property.getTenant() == null) {
            throw forbidden(
                    "Tai khoan nhan vien chua duoc gan Property hop le"
            );
        }

        Tenant tenant = property.getTenant();

        if (tenant == null
                || tenant.getStatus()
                != Tenant.TenantStatus.ACTIVE) {

            throw forbidden(
                    "Tenant hien khong hoat dong"
            );
        }

        // 6. Kiểm tra Subscription.
        Subscription subscription =
                subscriptionRepository.findByTenant(tenant)
                        .orElseThrow(() -> forbidden(
                                "Tenant chua co Subscription"
                        ));

        LocalDate today = LocalDate.now();

        if (subscription.getStatus()
                != Subscription.SubscriptionStatus.ACTIVE
                || subscription.getStartDate() == null
                || subscription.getEndDate() == null
                || subscription.getStartDate().isAfter(today)
                || subscription.getEndDate().isBefore(today)) {

            throw forbidden(
                    "Subscription khong con hieu luc"
            );
        }

        // 7. Kiểm tra Plan.
        SubscriptionPlan plan = subscription.getPlan();

        if (plan == null
                || plan.getStatus()
                != SubscriptionPlan.PlanStatus.ACTIVE) {

            throw forbidden(
                    "Subscription Plan khong hoat dong"
            );
        }

        // 8. Lấy Permission ACTIVE từ RolePermission.
        List<String> permissions =
                rolePermissionRepository
                        .findAllByRole_RoleIdAndPermission_Status(
                                user.getRole().getRoleId(),
                                Permission.PermissionStatus.ACTIVE
                        )
                        .stream()
                        .map(RolePermission::getPermission)
                        .map(Permission::getCode)
                        .distinct()
                        .sorted()
                        .toList();

        // 9. Tạo JWT cho PMS Employee.
        String token = jwtHelper.generatePmsEmployeeToken(
                user.getEmail(),
                user.getUserId(),
                property.getPropertyId(),
                tenant.getTenantId(),
                user.getRole().getName()
        );

        // 10. Trả kết quả đăng nhập.
        return new PmsEmployeeLoginResponse(
                "Login success",
                token,
                new PmsEmployeeLoginResponse.UserInfo(
                        user.getUserId(),
                        user.getFullName(),
                        user.getEmail(),
                        property.getPropertyId(),
                        property.getName(),
                        tenant.getTenantId(),
                        user.getRole().getName()
                ),
                permissions
        );
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Email hoac mat khau khong dung"
        );
    }

    private ResponseStatusException forbidden(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                message
        );
    }
}