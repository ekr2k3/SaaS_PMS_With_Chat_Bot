
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findAllByProperty_PropertyIdOrderByUserIdAsc(
            Long propertyId
    );

    Optional<User> findByUserIdAndProperty_PropertyId(
            Long userId,
            Long propertyId
    );

    @EntityGraph(attributePaths = {
            "role",
            "property",
            "property.tenant"
    })
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByProperty_Tenant_TenantId(Long tenantId);

    long countByProperty_PropertyIdAndRole_NameAndStatus(
            Long propertyId,
            String roleName,
            User.UserStatus status
    );
}