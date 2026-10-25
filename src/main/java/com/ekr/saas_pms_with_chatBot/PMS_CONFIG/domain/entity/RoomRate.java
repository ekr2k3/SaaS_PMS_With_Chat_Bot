
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.enums.RateName;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Entity
@Table(
        name = "room_rate",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_room_rate_room_type_rate_name",
                        columnNames = {"room_type_id", "rate_name"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_room_rate_room_type",
                        columnList = "room_type_id"
                )
        }
)
@Check(constraints = "price >= 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_rate_id")
    private Long roomRateId;

    /**
     * Tên bảng giá lấy từ danh sách cố định do SaaS quy định.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rate_name", nullable = false, length = 100)
    private RateName rateName;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;
}