package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "reservation_room_rate_snapshot",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reservation_room_rate_snapshot_night",
                        columnNames = {"reservation_id", "room_id", "stay_date"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_reservation_room_rate_snapshot_room",
                        columnList = "reservation_id, room_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ReservationRoomRateSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    /**
     * Chỉ lưu ID lịch sử.
     * Không tạo FK tới room_rate.
     */
    @Column(name = "room_rate_id", nullable = false)
    private Long roomRateId;

    @Column(name = "rate_name", nullable = false, length = 100)
    private String rateName;

    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;

    @Column(
            name = "price",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(
                    name = "reservation_id",
                    referencedColumnName = "reservation_id",
                    insertable = false,
                    updatable = false
            ),
            @JoinColumn(
                    name = "room_id",
                    referencedColumnName = "room_id",
                    insertable = false,
                    updatable = false
            )
    })
    private ReservationRoom reservationRoom;
}
