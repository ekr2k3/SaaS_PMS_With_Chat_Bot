package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository
        extends JpaRepository<Subscription, Long> {

    boolean existsByTenant(Tenant tenant);

    Optional<Subscription> findByTenant(Tenant tenant);

    List<Subscription> findByStatus(
            Subscription.SubscriptionStatus status
    );

    @Query("""
        SELECT s
        FROM Subscription s
        JOIN FETCH s.tenant t
        JOIN FETCH t.account a
        WHERE s.status = :status
          AND s.startDate IS NOT NULL
          AND s.endDate IS NOT NULL
          AND s.startDate <= :today
          AND s.startDate <= s.endDate
          AND s.endDate >= :startOfNextMonth
          AND s.endDate < :startOfMonthAfterNext
        """)
    List<Subscription> findRenewalCandidates(
            @Param("status")
            Subscription.SubscriptionStatus status,

            @Param("today")
            LocalDate today,

            @Param("startOfNextMonth")
            LocalDate startOfNextMonth,

            @Param("startOfMonthAfterNext")
            LocalDate startOfMonthAfterNext
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Subscription s
        JOIN FETCH s.tenant t
        JOIN FETCH t.account a
        JOIN FETCH s.plan p
        WHERE s.subscriptionId = :subscriptionId
        """)
    Optional<Subscription> findByIdForUpdate(
            @Param("subscriptionId") Long subscriptionId
    );

    // Dùng khi tạo Property để khóa Subscription theo Tenant.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Subscription s
        JOIN FETCH s.tenant t
        JOIN FETCH s.plan p
        WHERE t.tenantId = :tenantId
        """)
    Optional<Subscription> findByTenantIdForUpdate(
            @Param("tenantId") Long tenantId
    );

    @Query("""
        SELECT s
        FROM Subscription s
        JOIN FETCH s.tenant t
        JOIN FETCH t.account a
        JOIN FETCH s.plan p
        WHERE s.status = :status
          AND s.endDate IS NOT NULL
          AND s.endDate < :today
        ORDER BY s.endDate ASC
        """)
    List<Subscription> findExpiredCandidates(
            @Param("status")
            Subscription.SubscriptionStatus status,

            @Param("today")
            LocalDate today
    );
}