package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkin;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.filter.PmsEmployeePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CheckInAccessService {

    private static final String RECEPTIONIST_ROLE = "RECEPTIONIST";

    private final PropertyRepository propertyRepository;

    public CheckInAccessService(
            PropertyRepository propertyRepository
    ) {
        this.propertyRepository = propertyRepository;
    }

    /**
     * Kiểm tra nhân viên có quyền thực hiện nghiệp vụ
     * trên Property được chỉ định hay không.
     *
     * @param propertyId Property đang thao tác
     * @param permissionCode Permission cần có
     */
    public PmsEmployeePrincipal requireReceptionist(
            Long propertyId,
            String permissionCode
    ) {

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
                    "Only PMS employees can access Check-in"
            );
        }

        // 3. Kiểm tra Role.
        if (employee.role() == null
                || !RECEPTIONIST_ROLE.equalsIgnoreCase(
                employee.role()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only RECEPTIONIST employees can access Check-in"
            );
        }

        // 4. Kiểm tra Property được phân công.
        if (employee.propertyId() == null
                || !employee.propertyId().equals(propertyId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have access to this Property"
            );
        }

        // 5. Kiểm tra Permission.
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

        // 6. Kiểm tra Property thuộc Tenant.
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