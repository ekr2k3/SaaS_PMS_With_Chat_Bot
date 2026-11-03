package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.get;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GetReservationValidator {

    public void validate(
            String reservationCode,
            String email,
            String phone
    ) {

        if (reservationCode == null || reservationCode.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reservation code is required"
            );
        }

        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPhone = phone != null && !phone.isBlank();

        if (!hasEmail && !hasPhone) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email or phone is required"
            );
        }

        if (hasEmail && email.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is too long"
            );
        }

        if (hasEmail && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid email format"
            );
        }

        if (hasPhone && phone.length() > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phone is too long"
            );
        }
    }
}
