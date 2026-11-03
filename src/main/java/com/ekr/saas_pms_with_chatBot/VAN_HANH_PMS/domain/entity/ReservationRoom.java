package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Room;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "reservation_room")
@IdClass(ReservationRoom.ReservationRoomId.class)
@Getter
@Setter
@NoArgsConstructor
public class ReservationRoom {

    @Id
    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Id
    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "reservation_id",
            referencedColumnName = "reservation_id",
            insertable = false,
            updatable = false
    )
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "room_id",
            referencedColumnName = "room_id",
            insertable = false,
            updatable = false
    )
    private Room room;

    public static class ReservationRoomId implements Serializable {

        private Long reservationId;
        private Long roomId;

        public ReservationRoomId() {
        }

        public ReservationRoomId(Long reservationId, Long roomId) {
            this.reservationId = reservationId;
            this.roomId = roomId;
        }

        public Long getReservationId() {
            return reservationId;
        }

        public void setReservationId(Long reservationId) {
            this.reservationId = reservationId;
        }

        public Long getRoomId() {
            return roomId;
        }

        public void setRoomId(Long roomId) {
            this.roomId = roomId;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof ReservationRoomId that)) {
                return false;
            }
            return Objects.equals(reservationId, that.reservationId)
                    && Objects.equals(roomId, that.roomId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(reservationId, roomId);
        }
    }
}
