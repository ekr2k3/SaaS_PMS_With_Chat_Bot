package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.guestfnb.viewmenu;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.FnbMenu;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PropertyService;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.ServiceCatalog;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.FnbMenuRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyServiceRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.ServiceCatalogRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.guestfnb.access.GuestReservationContext;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.guestfnb.access.ResolveGuestReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ViewGuestFnbMenuService {

    private static final String ACTIVE = "ACTIVE";
    private static final String FNB = "FNB";

    private final ResolveGuestReservationService resolveGuestReservationService;
    private final FnbMenuRepository fnbMenuRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final PropertyServiceRepository propertyServiceRepository;

    public ViewGuestFnbMenuService(
            ResolveGuestReservationService resolveGuestReservationService,
            FnbMenuRepository fnbMenuRepository,
            ServiceCatalogRepository serviceCatalogRepository,
            PropertyServiceRepository propertyServiceRepository
    ) {
        this.resolveGuestReservationService = resolveGuestReservationService;
        this.fnbMenuRepository = fnbMenuRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.propertyServiceRepository = propertyServiceRepository;
    }

    @Transactional(readOnly = true)
    public List<GuestFnbMenuResponse> getMenu(
            String reservationCode,
            String email,
            String phone
    ) {

        GuestReservationContext context =
                resolveGuestReservationService.resolve(
                        reservationCode,
                        email,
                        phone
                );

        if (context.reservation().getStatus()
                != ReservationStatus.CHECKED_IN) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chỉ Reservation đã CHECKED_IN mới được sử dụng dịch vụ F&B"
            );
        }

        Long propertyId = context.reservation()
                .getProperty()
                .getPropertyId();

        validateFnbService(propertyId);

        List<FnbMenu> menus =
                fnbMenuRepository
                        .findAllByProperty_PropertyIdOrderByFnbMenuIdAsc(
                                propertyId
                        );

        return menus.stream()
                .filter(menu -> ACTIVE.equals(menu.getStatus()))
                .filter(menu -> menu.getActualQuantity() != null)
                .filter(menu -> menu.getActualQuantity() > 0)
                .map(GuestFnbMenuResponse::from)
                .toList();
    }

    private void validateFnbService(Long propertyId) {

        ServiceCatalog catalog = serviceCatalogRepository
                .findByServiceType(FNB)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Dịch vụ F&B chưa được cấu hình"
                ));

        if (!ACTIVE.equals(catalog.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Dịch vụ F&B đã ngừng cung cấp"
            );
        }

        PropertyService propertyService =
                propertyServiceRepository
                        .findByProperty_PropertyIdAndService_ServiceId(
                                propertyId,
                                catalog.getServiceId()
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Property chưa đăng ký dịch vụ F&B"
                        ));

        if (!ACTIVE.equals(propertyService.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Dịch vụ F&B của Property chưa được kích hoạt"
            );
        }
    }
}