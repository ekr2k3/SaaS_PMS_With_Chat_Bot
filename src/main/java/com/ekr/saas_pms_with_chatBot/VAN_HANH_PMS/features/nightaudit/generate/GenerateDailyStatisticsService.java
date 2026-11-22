package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.generate;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.DailyStatistics;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.DailyStatisticsStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.DailyStatisticsRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.FolioRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationPaymentRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRoomRateSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateDailyStatisticsService {

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private static final List<ReservationStatus> SUCCESSFUL_STATUSES =
            List.of(
                    ReservationStatus.CONFIRMED,
                    ReservationStatus.CHECKED_IN,
                    ReservationStatus.CHECKED_OUT
            );

    private static final List<ReservationStatus> ROOM_BLOCKING_STATUSES =
            List.of(
                    ReservationStatus.CONFIRMED,
                    ReservationStatus.CHECKED_IN
            );

    private static final List<ReservationPaymentType> COLLECTION_TYPES =
            List.of(
                    ReservationPaymentType.DEPOSIT,
                    ReservationPaymentType.BALANCE
            );

    private final PropertyRepository propertyRepository;
    private final DailyStatisticsRepository dailyStatisticsRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationPaymentRepository paymentRepository;
    private final ReservationRoomRateSnapshotRepository snapshotRepository;
    private final RoomRepository roomRepository;
    private final FolioRepository folioRepository;

    /**
     * Tạo Daily Statistics cho tất cả Property trong ngày nghiệp vụ.
     *
     * Nếu báo cáo đã tồn tại thì bỏ qua, không ghi đè dữ liệu
     * đã được Manager điều chỉnh hoặc xác nhận.
     */
    @Transactional
    public void generateForAllProperties(LocalDate businessDate) {
        List<Property> properties = propertyRepository.findAll();

        LocalDateTime from = businessDate.atStartOfDay();
        LocalDateTime to = businessDate.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);

        int createdCount = 0;
        int skippedCount = 0;

        for (Property property : properties) {
            Long propertyId = property.getPropertyId();

            if (dailyStatisticsRepository
                    .existsByProperty_PropertyIdAndBusinessDate(
                            propertyId,
                            businessDate
                    )) {
                skippedCount++;

                log.info(
                        "Bỏ qua Daily Statistics đã tồn tại: propertyId={}, businessDate={}",
                        propertyId,
                        businessDate
                );

                continue;
            }

            DailyStatistics statistics = generateForProperty(
                    property,
                    businessDate,
                    from,
                    to,
                    now
            );

            dailyStatisticsRepository.save(statistics);
            createdCount++;

            log.info(
                    "Đã tạo Daily Statistics: propertyId={}, businessDate={}",
                    propertyId,
                    businessDate
            );
        }

        log.info(
                "Hoàn tất tạo Daily Statistics: businessDate={}, created={}, skipped={}",
                businessDate,
                createdCount,
                skippedCount
        );
    }

    private DailyStatistics generateForProperty(
            Property property,
            LocalDate businessDate,
            LocalDateTime from,
            LocalDateTime to,
            LocalDateTime now
    ) {
        Long propertyId = property.getPropertyId();

        DailyStatistics statistics = new DailyStatistics();
        statistics.setProperty(property);
        statistics.setBusinessDate(businessDate);
        statistics.setStatus(DailyStatisticsStatus.REVIEWING);

        long successfulReservationCount =
                reservationRepository.countSuccessfulReservationsCreatedInDate(
                        propertyId,
                        from,
                        to,
                        SUCCESSFUL_STATUSES
                );
        statistics.setSuccessfulReservationCount(
                Math.toIntExact(successfulReservationCount)
        );

        statistics.setDepositAmount(
                zeroIfNull(
                        reservationRepository.sumDepositAmountCreatedInDate(
                                propertyId,
                                from,
                                to
                        )
                )
        );

        statistics.setDepositCollected(
                sumPayment(
                        propertyId,
                        ReservationPaymentType.DEPOSIT,
                        from,
                        to
                )
        );

        statistics.setRefundTotal(
                sumPayment(
                        propertyId,
                        ReservationPaymentType.REFUND,
                        from,
                        to
                )
        );

        statistics.setExpectedRoomRevenue(
                zeroIfNull(
                        snapshotRepository.sumExpectedRoomRevenue(
                                propertyId,
                                from,
                                to,
                                SUCCESSFUL_STATUSES
                        )
                )
        );

        statistics.setFnbRevenue(
                zeroIfNull(
                        folioRepository.sumFnbRevenue(
                                propertyId,
                                from,
                                to
                        )
                )
        );

        statistics.setFnbExpense(BigDecimal.ZERO);

        long occupiedRoomCount =
                roomRepository.countOccupiedRooms(
                        propertyId,
                        businessDate,
                        ReservationStatus.CHECKED_IN
                );
        statistics.setOccupiedRoomCount(
                Math.toIntExact(occupiedRoomCount)
        );

        long availableRoomCount =
                roomRepository.countAvailableRoomsForBusinessDate(
                        propertyId,
                        businessDate,
                        now,
                        ROOM_BLOCKING_STATUSES,
                        ReservationStatus.PENDING_PAYMENT
                );
        statistics.setAvailableRoomCount(
                Math.toIntExact(availableRoomCount)
        );

        Long guestCount = reservationRepository.sumCheckedInGuestCount(
                propertyId,
                businessDate,
                ReservationStatus.CHECKED_IN
        );
        statistics.setGuestCount(
                Math.toIntExact(guestCount == null ? 0L : guestCount)
        );

        long cancelledCount =
                paymentRepository.countReservationsByPaymentDateAndReservationStatus(
                        propertyId,
                        COLLECTION_TYPES,
                        ReservationPaymentStatus.SUCCEEDED,
                        from,
                        to,
                        ReservationStatus.CANCELLED
                );
        statistics.setCancelledReservationCount(
                Math.toIntExact(cancelledCount)
        );

        long noShowCount =
                paymentRepository.countReservationsByPaymentDateAndReservationStatus(
                        propertyId,
                        COLLECTION_TYPES,
                        ReservationPaymentStatus.SUCCEEDED,
                        from,
                        to,
                        ReservationStatus.NO_SHOW
                );
        statistics.setNoShowCount(Math.toIntExact(noShowCount));

        return statistics;
    }

    private BigDecimal sumPayment(
            Long propertyId,
            ReservationPaymentType paymentType,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return zeroIfNull(
                paymentRepository.sumSuccessfulPaymentAmount(
                        propertyId,
                        paymentType,
                        ReservationPaymentStatus.SUCCEEDED,
                        from,
                        to
                )
        );
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
