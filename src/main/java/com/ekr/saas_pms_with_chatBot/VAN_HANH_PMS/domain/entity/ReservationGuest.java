package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "reservation_guest")
@IdClass(ReservationGuest.ReservationGuestId.class)
@Getter
@Setter
@NoArgsConstructor
public class ReservationGuest {

    @Id
    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Id
    @Column(name = "guest_id", nullable = false)
    private Long guestId;

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
            name = "guest_id",
            referencedColumnName = "guest_id",
            insertable = false,
            updatable = false
    )
    private Guest guest;

    public static class ReservationGuestId implements Serializable {

        private Long reservationId;
        private Long guestId;

        public ReservationGuestId() {
        }

        public ReservationGuestId(Long reservationId, Long guestId) {
            this.reservationId = reservationId;
            this.guestId = guestId;
        }

        public Long getReservationId() {
            return reservationId;
        }

        public void setReservationId(Long reservationId) {
            this.reservationId = reservationId;
        }

        public Long getGuestId() {
            return guestId;
        }

        public void setGuestId(Long guestId) {
            this.guestId = guestId;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof ReservationGuestId that)) {
                return false;
            }
            return Objects.equals(reservationId, that.reservationId)
                    && Objects.equals(guestId, that.guestId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(reservationId, guestId);
        }
    }
}
