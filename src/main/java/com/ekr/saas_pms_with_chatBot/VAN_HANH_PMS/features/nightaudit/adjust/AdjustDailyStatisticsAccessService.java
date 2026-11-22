package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.adjust;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.filter.PmsEmployeePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdjustDailyStatisticsAccessService {

    private static final String MANAGER_ROLE = "MANAGER";
    private static final String REQUIRED_PERMISSION = "PERM_NIGHT_AUDIT_ADJUST";

    private final PropertyRepository propertyRepository;

    public AdjustDailyStatisticsAccessService(
            PropertyRepository propertyRepository
    ) {
        this.propertyRepository = propertyRepository;
    }

    public PmsEmployeePrincipal requireAccess(Long propertyId) {
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

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof PmsEmployeePrincipal employee)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only PMS employees can adjust Night Audit"
            );
        }

        if (employee.role() == null
                || !MANAGER_ROLE.equalsIgnoreCase(employee.role())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only MANAGER can adjust Night Audit"
            );
        }

        if (employee.propertyId() == null
                || !employee.propertyId().equals(propertyId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have access to this Property"
            );
        }

        boolean hasPermission =
                employee.getAuthorities() != null
                        && employee.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                REQUIRED_PERMISSION.equals(
                                        authority.getAuthority()
                                )
                        );

        if (!hasPermission) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Missing permission: NIGHT_AUDIT_ADJUST"
            );
        }

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
