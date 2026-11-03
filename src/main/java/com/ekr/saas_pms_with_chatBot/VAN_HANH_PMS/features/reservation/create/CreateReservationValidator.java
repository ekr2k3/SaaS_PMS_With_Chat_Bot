package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.create;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Component
public class CreateReservationValidator {

    public void validate(CreateReservationRequest request) {

        LocalDate today = LocalDate.now();

        if (!request.checkInDate().isBefore(request.checkOutDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Check-out date must be after check-in date"
            );
        }

        if (request.checkInDate().isBefore(today)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Check-in date cannot be in the past"
            );
        }

        if (request.rooms() == null || request.rooms().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one room must be selected"
            );
        }

        Set<Long> roomIds = new HashSet<>();

        for (CreateReservationRequest.RoomSelection room
                : request.rooms()) {

            if (!roomIds.add(room.roomId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate room ID: " + room.roomId()
                );
            }
        }
    }
}
