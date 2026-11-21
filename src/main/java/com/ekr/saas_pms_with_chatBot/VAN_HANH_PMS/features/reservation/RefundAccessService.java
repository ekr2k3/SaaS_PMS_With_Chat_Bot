package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.filter.PmsEmployeePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RefundAccessService {
    private final PropertyRepository properties;
    public RefundAccessService(PropertyRepository properties){this.properties=properties;}
    public PmsEmployeePrincipal requirePermission(Long propertyId,String permissionCode){
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");
        if(!(auth.getPrincipal() instanceof PmsEmployeePrincipal employee))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only PMS employees can access this operation");
        if(employee.propertyId()==null || !employee.propertyId().equals(propertyId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You do not have access to this Property");
        boolean allowed=employee.getAuthorities()!=null && employee.getAuthorities().stream()
                .anyMatch(a -> ("PERM_"+permissionCode).equals(a.getAuthority()));
        if(!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Missing permission: "+permissionCode);
        properties.findByPropertyIdAndTenant_TenantId(propertyId,employee.tenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Property not found"));
        return employee;
    }
}
