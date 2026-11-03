package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.createpayment;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

@Component
public class CreateReservationPaymentValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public void validate(String reservationCode, String email, String phone) {

        if (reservationCode == null || reservationCode.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reservation code không hợp lệ"
            );
        }

        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPhone = phone != null && !phone.isBlank();

        if (!hasEmail && !hasPhone) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng cung cấp email hoặc số điện thoại"
            );
        }

        if (hasEmail) {
            email = email.trim();

            if (email.length() > 255 ||
                    !EMAIL_PATTERN.matcher(email).matches()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Email không hợp lệ"
                );
            }
        }

        if (hasPhone) {
            phone = phone.trim();

            if (phone.length() > 20) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Số điện thoại không hợp lệ"
                );
            }
        }
    }
}
