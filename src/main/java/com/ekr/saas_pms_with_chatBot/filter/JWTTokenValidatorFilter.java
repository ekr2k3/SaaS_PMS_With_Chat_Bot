
package com.ekr.saas_pms_with_chatBot.filter;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Permission;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RolePermission;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.User;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RolePermissionRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.UserRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class JWTTokenValidatorFilter extends OncePerRequestFilter {

    @Value("${jwt.secret:saasPmsJwtSecretKey2026ProductionSecureKey_9fK3mX7qL2vN8cR5}")
    private String secret;

    @Value("${acc.user}")
    private String admin_u;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);

        try {
            SecretKey key = Keys.hmacShaKeyFor(
                    secret.getBytes()
            );

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            String actorType = claims.get("actor_type", String.class);

            if ("PMS_EMPLOYEE".equals(actorType)) {

                authenticatePmsEmployee(claims, subject);

            } else {

                authenticateSaasUser(subject);
            }

        } catch (EmployeeAccessDeniedException e) {

            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    e.getMessage()
            );
            return;

        } catch (Exception e) {

            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired access token"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Xác thực SaaS Admin và SaaS Owner.
     * Giữ nguyên cách nhận diện hiện tại của hệ thống.
     */
    private void authenticateSaasUser(String email) {

        String role;

        if (Objects.equals(admin_u, email)) {

            role = "ROLE_ADMIN";

        } else {

            Account account = accountRepository.findByEmail(email);

            if (account == null) {
                throw new IllegalArgumentException(
                        "Account does not exist"
                );
            }

            role = "ROLE_OWNER";
        }

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(email)
                        .password("")
                        .authorities(role)
                        .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    /**
     * Xác thực nhân viên PMS.
     *
     * Không sử dụng Role/Property/Tenant lấy trực tiếp từ JWT
     * làm dữ liệu phân quyền. Các thông tin này được đọc lại từ DB.
     */
    private void authenticatePmsEmployee(
            Claims claims,
            String email
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "PMS Employee does not exist"
                ));

        // 1. Kiểm tra User.
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new EmployeeAccessDeniedException(
                    "PMS Employee account is inactive"
            );
        }

        // 2. Kiểm tra Role.
        if (user.getRole() == null
                || user.getRole().getStatus()
                != com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Role.RoleStatus.ACTIVE) {

            throw new EmployeeAccessDeniedException(
                    "PMS Employee role is inactive"
            );
        }

        String roleName = user.getRole().getName();

        if ("OWNER".equalsIgnoreCase(roleName)) {
            throw new EmployeeAccessDeniedException(
                    "Owner must use SaaS authentication"
            );
        }

        // 3. Kiểm tra Property và Tenant.
        if (user.getProperty() == null
                || user.getProperty().getTenant() == null) {

            throw new EmployeeAccessDeniedException(
                    "PMS Employee has no valid Property"
            );
        }

        Long propertyId = user.getProperty().getPropertyId();

        Tenant tenant = user.getProperty().getTenant();

        Long tenantId = tenant.getTenantId();

        if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
            throw new EmployeeAccessDeniedException(
                    "Tenant is inactive"
            );
        }

        // 4. Đối chiếu các ID trong JWT với dữ liệu hiện tại.
        if (!sameId(claims.get("user_id"), user.getUserId())
                || !sameId(claims.get("property_id"), propertyId)
                || !sameId(claims.get("tenant_id"), tenantId)) {

            throw new IllegalArgumentException(
                    "Token context no longer matches current account data"
            );
        }

        // 5. Kiểm tra Subscription.
        Subscription subscription =
                subscriptionRepository.findByTenant(tenant)
                        .orElseThrow(() ->
                                new EmployeeAccessDeniedException(
                                        "Tenant has no Subscription"
                                )
                        );

        LocalDate today = LocalDate.now();

        if (subscription.getStatus()
                != Subscription.SubscriptionStatus.ACTIVE
                || subscription.getStartDate() == null
                || subscription.getEndDate() == null
                || subscription.getStartDate().isAfter(today)
                || subscription.getEndDate().isBefore(today)) {

            throw new EmployeeAccessDeniedException(
                    "Subscription is not active"
            );
        }

        // 6. Kiểm tra Plan.
        SubscriptionPlan plan = subscription.getPlan();

        if (plan == null
                || plan.getStatus()
                != SubscriptionPlan.PlanStatus.ACTIVE) {

            throw new EmployeeAccessDeniedException(
                    "Subscription Plan is inactive"
            );
        }

        // 7. Nạp Permission mới nhất từ DB.
        List<SimpleGrantedAuthority> authorities =
                new ArrayList<>();

        authorities.add(
                new SimpleGrantedAuthority(
                        "ROLE_" + roleName.toUpperCase()
                )
        );

        List<RolePermission> rolePermissions =
                rolePermissionRepository
                        .findAllByRole_RoleIdAndPermission_Status(
                                user.getRole().getRoleId(),
                                Permission.PermissionStatus.ACTIVE
                        );

        for (RolePermission rolePermission : rolePermissions) {

            String permissionCode =
                    rolePermission.getPermission().getCode();

            authorities.add(
                    new SimpleGrantedAuthority(
                            "PERM_" + permissionCode
                    )
            );
        }

        // 8. Tạo Principal chứa thông tin PMS Employee.
        PmsEmployeePrincipal principal =
                new PmsEmployeePrincipal(
                        user.getUserId(),
                        user.getEmail(),
                        propertyId,
                        tenantId,
                        roleName,
                        authorities
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    private boolean sameId(Object claimValue, Long databaseValue) {

        if (!(claimValue instanceof Number number)
                || databaseValue == null) {
            return false;
        }

        return number.longValue() == databaseValue;
    }

    /**
     * Không cần JWT tại endpoint đăng nhập.
     */
    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        String path = request.getServletPath();

        if (path.equals("/api/v1/saas/user/login")) return true;
        if (path.equals("/api/v1/pms/auth/login")) return true;
        if (path.equals("/api/v1/saas/user/register")) return true;

        if (path.equals("/api/v1/saas/user/Plan/List")) return true;
        if (path.equals("/api/v1/saas/user/Plan/Detail")) return true;

        if (path.equals("/api/webhooks/stripe")) return true;
        if (path.equals("/api/payments/stripe/success")) return true;
        if (path.equals("/api/payments/stripe/cancel")) return true;

        if (path.startsWith("/api/public/")) return true;

        if (path.equals("/test_llm")) return true;

        return false;
    }

    private static class EmployeeAccessDeniedException
            extends RuntimeException {

        private EmployeeAccessDeniedException(String message) {
            super(message);
        }
    }
}