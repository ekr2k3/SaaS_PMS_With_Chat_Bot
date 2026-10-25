
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "room_type_amenity")
@IdClass(RoomTypeAmenity.RoomTypeAmenityId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeAmenity {

    @Id
    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Id
    @Column(name = "amenity", nullable = false, length = 100)
    private String amenity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "room_type_id",
            insertable = false,
            updatable = false,
            nullable = false
    )
    private RoomType roomType;

    public static class RoomTypeAmenityId implements Serializable {

        private Long roomTypeId;
        private String amenity;

        public RoomTypeAmenityId() {
        }

        public RoomTypeAmenityId(Long roomTypeId, String amenity) {
            this.roomTypeId = roomTypeId;
            this.amenity = amenity;
        }

        public Long getRoomTypeId() {
            return roomTypeId;
        }

        public void setRoomTypeId(Long roomTypeId) {
            this.roomTypeId = roomTypeId;
        }

        public String getAmenity() {
            return amenity;
        }

        public void setAmenity(String amenity) {
            this.amenity = amenity;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }

            if (!(object instanceof RoomTypeAmenityId that)) {
                return false;
            }

            return Objects.equals(roomTypeId, that.roomTypeId)
                    && Objects.equals(amenity, that.amenity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roomTypeId, amenity);
        }
    }
}