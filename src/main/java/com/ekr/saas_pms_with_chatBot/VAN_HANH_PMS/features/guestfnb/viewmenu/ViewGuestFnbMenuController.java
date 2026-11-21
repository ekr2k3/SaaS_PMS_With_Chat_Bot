package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.guestfnb.viewmenu;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/reservations")
public class ViewGuestFnbMenuController {

    private final ViewGuestFnbMenuService service;

    public ViewGuestFnbMenuController(
            ViewGuestFnbMenuService service
    ) {
        this.service = service;
    }

    @GetMapping("/{reservationCode}/fnb-menu")
    public ResponseEntity<List<GuestFnbMenuResponse>> getMenu(

            @PathVariable String reservationCode,

            @RequestParam(required = false) String email,

            @RequestParam(required = false) String phone

    ) {

        return ResponseEntity.ok(
                service.getMenu(
                        reservationCode,
                        email,
                        phone
                )
        );
    }
}