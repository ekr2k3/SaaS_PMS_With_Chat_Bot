package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.onlinereservation.SearchAvailableRooms;

import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/public/properties/{propertyId}/available-rooms")
@Validated
public class SearchAvailableRoomsController {

    private final SearchAvailableRoomsService searchAvailableRoomsService;

    public SearchAvailableRoomsController(
            SearchAvailableRoomsService searchAvailableRoomsService
    ) {
        this.searchAvailableRoomsService = searchAvailableRoomsService;
    }

    /**
     * GET /api/public/properties/{propertyId}/available-rooms
     *
     * Tìm các Room còn khả dụng theo:
     * - Check-in date
     * - Check-out date
     * - Guest count
     * - Room Type (optional)
     */
    @GetMapping
    public SearchAvailableRoomsResponse searchAvailableRooms(
            @PathVariable
            @Positive
            Long propertyId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkInDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkOutDate,

            @RequestParam
            @Positive
            Integer guestCount,

            @RequestParam(required = false)
            @Positive
            Long roomTypeId
    ) {
        return searchAvailableRoomsService.search(
                propertyId,
                checkInDate,
                checkOutDate,
                guestCount,
                roomTypeId
        );
    }
}
