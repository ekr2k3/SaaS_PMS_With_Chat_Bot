package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Folio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface FolioRepository
        extends JpaRepository<Folio, Long> {

    List<Folio>
    findAllByReservation_ReservationIdOrderByFolioIdAsc(
            Long reservationId
    );

    List<Folio>
    findAllByGuest_GuestIdOrderByFolioIdAsc(
            Long guestId
    );

    @Query("""
            SELECT COALESCE(SUM(folio.amount), 0)
            FROM Folio folio
            WHERE folio.reservation.property.propertyId = :propertyId
              AND folio.service = 'FNB'
              AND folio.createdAt >= :from
              AND folio.createdAt < :to
            """)
    BigDecimal sumFnbRevenue(
            @Param("propertyId") Long propertyId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}