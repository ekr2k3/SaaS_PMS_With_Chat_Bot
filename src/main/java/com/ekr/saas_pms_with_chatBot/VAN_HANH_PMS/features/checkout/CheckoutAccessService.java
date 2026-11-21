package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.filter.PmsEmployeePrincipal;

import org.springframework.http.HttpStatus;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;

import org.springframework.web.server.ResponseStatusException;

@Service
public class CheckoutAccessService {

    private final PropertyRepository propertyRepository;

    public CheckoutAccessService(
            PropertyRepository propertyRepository
    ) {
        this.propertyRepository = propertyRepository;
    }

    /**
     * Kiểm tra nhân viên có quyền thực hiện nghiệp vụ
     * trên Property được chỉ định.
     *
     * Không giới hạn cứng Role tại đây.
     * Quyền truy cập được xác định bằng Permission.
     */
    public PmsEmployeePrincipal requirePermission(
            Long propertyId,
            String permissionCode
    ) {

        if (propertyId == null || permissionCode == null
                || permissionCode.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Property ID hoặc Permission không hợp lệ"
            );
        }

        // 1. Kiểm tra Authentication.
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        // 2. Chỉ chấp nhận PMS Employee.
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof PmsEmployeePrincipal employee)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only PMS employees can access Checkout"
            );
        }

        // 3. Kiểm tra Property được phân công.
        if (employee.propertyId() == null
                || !employee.propertyId().equals(propertyId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have access to this Property"
            );
        }

        // 4. Kiểm tra Permission.
        String requiredAuthority = "PERM_" + permissionCode;

        boolean hasPermission =
                employee.getAuthorities() != null
                        && employee.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                requiredAuthority.equals(
                                        authority.getAuthority()
                                )
                        );

        if (!hasPermission) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Missing permission: " + permissionCode
            );
        }

        // 5. Kiểm tra Property thuộc Tenant.
        propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        employee.tenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Property not found"
                ));

        return employee;
    }
}
