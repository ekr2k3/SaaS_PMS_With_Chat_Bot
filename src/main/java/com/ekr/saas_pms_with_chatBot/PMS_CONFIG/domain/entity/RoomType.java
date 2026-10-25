
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.enums.RoomTypeName;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "room_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_type_id")
    private Long roomTypeId;

    /** Tên lấy từ danh sách cố định của SaaS. */
    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, length = 100)
    private RoomTypeName name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    /** Loại giường giữ dạng chuỗi theo schema đã thống nhất. */
    @Column(name = "bed_type", nullable = false, length = 50)
    private String bedType;

    @Column(name = "number_of_beds", nullable = false)
    private Integer numberOfBeds;

    @Column(name = "number_of_rooms", nullable = false)
    private Integer numberOfRooms;

    @Column(name = "image", length = 500)
    private String image;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Builder.Default
    @OneToMany(
            mappedBy = "roomType",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<RoomTypeAmenity> amenities = new ArrayList<>();
}
