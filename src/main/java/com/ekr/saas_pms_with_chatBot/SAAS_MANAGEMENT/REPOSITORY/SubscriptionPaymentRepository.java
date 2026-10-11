package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPayment;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentGateway;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionPaymentRepository
        extends JpaRepository<SubscriptionPayment, Long> {

    Optional<SubscriptionPayment>
    findFirstBySubscription_SubscriptionIdAndPaymentGatewayOrderByIdDesc(
            Long subscriptionId,
            PaymentGateway paymentGateway
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM SubscriptionPayment p
        WHERE p.id = :paymentId
    """)
    Optional<SubscriptionPayment> findByIdForUpdate(
            @Param("paymentId") Long paymentId
    );

    // Tìm Payment thành công cho đúng Subscription và kỳ dịch vụ.
    @Query("""
        SELECT p
        FROM SubscriptionPayment p
        WHERE p.subscription.subscriptionId = :subscriptionId
          AND p.status = :status
          AND p.periodStart = :periodStart
          AND p.periodEnd = :periodEnd
    """)
    List<SubscriptionPayment> findSuccessfulPaymentsForPeriod(
            @Param("subscriptionId") Long subscriptionId,
            @Param("status") PaymentStatus status,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );
}