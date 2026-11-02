
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.FnbMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FnbMenuRepository
        extends JpaRepository<FnbMenu, Long> {

    List<FnbMenu>
    findAllByProperty_PropertyIdOrderByFnbMenuIdAsc(
            Long propertyId
    );

    Optional<FnbMenu>
    findByFnbMenuIdAndProperty_PropertyId(
            Long fnbMenuId,
            Long propertyId
    );
}