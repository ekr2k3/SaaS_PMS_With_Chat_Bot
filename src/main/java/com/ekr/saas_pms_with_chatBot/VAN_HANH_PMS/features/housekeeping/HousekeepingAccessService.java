package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.housekeeping;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.filter.PmsEmployeePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HousekeepingAccessService {

    private static final String HOUSEKEEPER_ROLE = "HOUSEKEEPER";

    private final PropertyRepository propertyRepository;

    public HousekeepingAccessService(
            PropertyRepository propertyRepository
    ) {
        this.propertyRepository = propertyRepository;
    }

    public PmsEmployeePrincipal requireHousekeeper() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

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
                    "Only HOUSEKEEPER employees can access this feature"
            );
        }

        if (employee.role() == null
                || !HOUSEKEEPER_ROLE.equalsIgnoreCase(employee.role())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only employees with HOUSEKEEPER role are allowed"
            );
        }

        return employee;
    }

    public Property requirePropertyAccess(Long propertyId) {

        PmsEmployeePrincipal employee = requireHousekeeper();

        if (employee.propertyId() == null
                || !employee.propertyId().equals(propertyId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have access to this Property"
            );
        }

        return propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        employee.tenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Property not found"
                ));
    }
}