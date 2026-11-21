package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.FnbMenu;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m
            FROM FnbMenu m
            WHERE m.fnbMenuId IN :menuIds
              AND m.property.propertyId = :propertyId
            ORDER BY m.fnbMenuId ASC
            """)
    List<FnbMenu> findAllByFnbMenuIdInAndProperty_PropertyIdForUpdate(
            @Param("menuIds") List<Long> menuIds,
            @Param("propertyId") Long propertyId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m
            FROM FnbMenu m
            WHERE m.fnbMenuId = :menuId
              AND m.property.propertyId = :propertyId
            """)
    Optional<FnbMenu> findByFnbMenuIdAndProperty_PropertyIdForUpdate(
            @Param("menuId") Long menuId,
            @Param("propertyId") Long propertyId
    );
}