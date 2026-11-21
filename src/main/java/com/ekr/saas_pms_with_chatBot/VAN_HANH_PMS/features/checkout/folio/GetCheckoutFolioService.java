package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.folio;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Folio;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.FolioRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.checkout.CheckoutAccessService;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.StringUtils;

import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import java.util.List;

@Service
public class GetCheckoutFolioService {

    private static final String REQUIRED_PERMISSION =
            "CHECKOUT_VIEW";

    private final CheckoutAccessService accessService;

    private final ReservationRepository reservationRepository;

    private final FolioRepository folioRepository;

    public GetCheckoutFolioService(
            CheckoutAccessService accessService,
            ReservationRepository reservationRepository,
            FolioRepository folioRepository
    ) {
        this.accessService = accessService;
        this.reservationRepository = reservationRepository;
        this.folioRepository = folioRepository;
    }

    @Transactional(readOnly = true)
    public GetCheckoutFolioResponse getFolio(
            Long propertyId,
            String reservationCode
    ) {

        // 1. Kiểm tra quyền.
        accessService.requirePermission(
                propertyId,
                REQUIRED_PERMISSION
        );

        // 2. Kiểm tra Reservation Code.
        if (!StringUtils.hasText(reservationCode)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reservation Code không hợp lệ"
            );
        }

        // 3. Tìm Reservation.
        Reservation reservation = reservationRepository
                .findByReservationCode(reservationCode.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reservation not found"
                ));

        // 4. Kiểm tra Reservation thuộc Property.
        if (reservation.getProperty() == null
                || !propertyId.equals(
                        reservation.getProperty().getPropertyId()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reservation not found"
            );
        }

        // 5. Chỉ xem Checkout Folio khi khách đang lưu trú.
        if (reservation.getStatus()
                != ReservationStatus.CHECKED_IN) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation must be CHECKED_IN to view Checkout Folio"
            );
        }

        // 6. Lấy danh sách Folio.
        List<Folio> folios =
                folioRepository
                        .findAllByReservation_ReservationIdOrderByFolioIdAsc(
                                reservation.getReservationId()
                        );

        // 7. Tính tổng Folio.
        BigDecimal folioTotal = folios.stream()
                .map(Folio::getAmount)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        // 8. Chuyển đổi Response.
        List<GetCheckoutFolioResponse.FolioItem> items =
                folios.stream()
                        .map(this::toFolioItem)
                        .toList();

        return new GetCheckoutFolioResponse(
                reservation.getReservationId(),
                reservation.getReservationCode(),
                reservation.getStatus(),
                reservation.getCurrency(),
                items,
                folioTotal
        );
    }

    private GetCheckoutFolioResponse.FolioItem toFolioItem(
            Folio folio
    ) {

        return new GetCheckoutFolioResponse.FolioItem(
                folio.getFolioId(),
                folio.getService(),
                folio.getQuantity(),
                folio.getUnitPrice(),
                folio.getAmount(),
                folio.getServiceData()
        );
    }
}
