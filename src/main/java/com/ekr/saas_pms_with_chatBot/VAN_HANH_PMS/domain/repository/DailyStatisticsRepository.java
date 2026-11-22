package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.DailyStatistics;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyStatisticsRepository
        extends JpaRepository<DailyStatistics, Long> {

    Optional<DailyStatistics>
    findByProperty_PropertyIdAndBusinessDate(
            Long propertyId,
            LocalDate businessDate
    );

    List<DailyStatistics>
    findAllByProperty_PropertyIdOrderByBusinessDateDesc(
            Long propertyId
    );

    List<DailyStatistics>
    findAllByProperty_PropertyIdAndBusinessDateBetweenOrderByBusinessDateDesc(
            Long propertyId,
            LocalDate fromDate,
            LocalDate toDate
    );

    boolean existsByProperty_PropertyIdAndBusinessDate(
            Long propertyId,
            LocalDate businessDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ds
            FROM DailyStatistics ds
            WHERE ds.property.propertyId = :propertyId
              AND ds.businessDate = :businessDate
            """)
    Optional<DailyStatistics> findByPropertyAndBusinessDateForUpdate(
            @Param("propertyId") Long propertyId,
            @Param("businessDate") LocalDate businessDate
    );

    @Query("""
            SELECT ds
            FROM DailyStatistics ds
            JOIN ds.property p
            WHERE p.tenant.tenantId = :tenantId
              AND (:propertyId IS NULL OR p.propertyId = :propertyId)
              AND (:businessDate IS NULL OR ds.businessDate = :businessDate)
              AND (:fromDate IS NULL OR ds.businessDate >= :fromDate)
              AND (:toDate IS NULL OR ds.businessDate <= :toDate)
            ORDER BY ds.businessDate DESC, p.propertyId ASC
            """)
    List<DailyStatistics> findAllForTenantOwner(
            @Param("tenantId") Long tenantId,
            @Param("propertyId") Long propertyId,
            @Param("businessDate") LocalDate businessDate,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
