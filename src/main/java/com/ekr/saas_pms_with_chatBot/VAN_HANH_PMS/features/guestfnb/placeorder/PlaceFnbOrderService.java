package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.guestfnb.placeorder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.FnbMenu;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PropertyService;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.ServiceCatalog;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.FnbMenuRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyServiceRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.ServiceCatalogRepository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Folio;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Guest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.FolioRepository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.guestfnb.access.GuestReservationContext;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.guestfnb.access.ResolveGuestReservationService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PlaceFnbOrderService {

    private static final String ACTIVE = "ACTIVE";
    private static final String FNB = "FNB";

    private final ResolveGuestReservationService resolveGuestReservationService;
    private final FnbMenuRepository fnbMenuRepository;
    private final FolioRepository folioRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final PropertyServiceRepository propertyServiceRepository;
    private final ObjectMapper objectMapper;

    public PlaceFnbOrderService(
            ResolveGuestReservationService resolveGuestReservationService,
            FnbMenuRepository fnbMenuRepository,
            FolioRepository folioRepository,
            ServiceCatalogRepository serviceCatalogRepository,
            PropertyServiceRepository propertyServiceRepository,
            ObjectMapper objectMapper
    ) {
        this.resolveGuestReservationService = resolveGuestReservationService;
        this.fnbMenuRepository = fnbMenuRepository;
        this.folioRepository = folioRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.propertyServiceRepository = propertyServiceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PlaceFnbOrderResponse placeOrder(
            String reservationCode,
            String email,
            String phone,
            PlaceFnbOrderRequest request
    ) {

        validateRequest(request);

        // 1. Lock Reservation để tránh xử lý đồng thời
        // trên cùng Reservation trong lúc đặt dịch vụ.
        GuestReservationContext context =
                resolveGuestReservationService.resolveForUpdate(
                        reservationCode,
                        email,
                        phone
                );

        Reservation reservation = context.reservation();
        Guest guest = context.guest();

        // 2. Chỉ cho phép Reservation CHECKED_IN.
        if (reservation.getStatus()
                != ReservationStatus.CHECKED_IN) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation phải ở trạng thái CHECKED_IN"
            );
        }

        Long propertyId = reservation.getProperty()
                .getPropertyId();

        // 3. Kiểm tra dịch vụ F&B còn hoạt động.
        validateFnbService(propertyId);

        // 4. Không cho phép menuId trùng nhau trong một request.
        Map<Long, Integer> requestedItems =
                new LinkedHashMap<>();

        for (PlaceFnbOrderRequest.Item item : request.items()) {

            if (requestedItems.putIfAbsent(
                    item.menuId(),
                    item.quantity()
            ) != null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Không được gửi trùng menuId trong một đơn"
                );
            }
        }

        // 5. Sắp xếp ID để các request đồng thời
        // khóa Menu theo cùng một thứ tự.
        List<Long> menuIds = requestedItems.keySet()
                .stream()
                .sorted()
                .toList();

        // 6. Lock tất cả Menu thuộc đúng Property.
        List<FnbMenu> lockedMenus =
                fnbMenuRepository
                        .findAllByFnbMenuIdInAndProperty_PropertyIdForUpdate(
                                menuIds,
                                propertyId
                        );

        if (lockedMenus.size() != menuIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Một hoặc nhiều món không tồn tại trong Property"
            );
        }

        Map<Long, FnbMenu> menuMap = new LinkedHashMap<>();

        for (FnbMenu menu : lockedMenus) {
            menuMap.put(menu.getFnbMenuId(), menu);
        }

        // 7. Kiểm tra trạng thái và số lượng tồn.
        for (Long menuId : menuIds) {

            FnbMenu menu = menuMap.get(menuId);
            Integer requestedQuantity = requestedItems.get(menuId);

            if (!ACTIVE.equals(menu.getStatus())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Món " + menu.getName() + " đã ngừng kinh doanh"
                );
            }

            if (menu.getActualQuantity() == null
                    || menu.getActualQuantity() < requestedQuantity) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Không đủ tồn kho cho món " + menu.getName()
                );
            }

            if (menu.getSellingPrice() == null
                    || menu.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Giá bán của món không hợp lệ"
                );
            }
        }

        // 8. Tạo từng dòng Folio và trừ tồn kho.
        List<Folio> folios = new ArrayList<>();

        List<PlaceFnbOrderResponse.ItemResult> itemResults =
                new ArrayList<>();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Long menuId : menuIds) {

            FnbMenu menu = menuMap.get(menuId);

            Integer quantity = requestedItems.get(menuId);

            BigDecimal unitPrice = menu.getSellingPrice();

            BigDecimal amount = unitPrice.multiply(
                    BigDecimal.valueOf(quantity)
            );

            Folio folio = new Folio();

            folio.setService(FNB);
            folio.setQuantity(quantity);
            folio.setUnitPrice(unitPrice);
            folio.setAmount(amount);
            folio.setReservation(reservation);
            folio.setGuest(guest);

            folio.setServiceData(
                    createServiceData(
                            menu,
                            request.serviceTime()
                    )
            );

            folios.add(folio);

            // Chỉ trừ actualQuantity.
            // Không thay đổi maxQuantity.
            menu.setActualQuantity(
                    menu.getActualQuantity() - quantity
            );

            totalAmount = totalAmount.add(amount);

            itemResults.add(
                    new PlaceFnbOrderResponse.ItemResult(
                            menu.getFnbMenuId(),
                            menu.getName(),
                            quantity,
                            unitPrice,
                            amount,
                            menu.getActualQuantity(),
                            null
                    )
            );
        }

        // 9. Ghi Folio.
        List<Folio> savedFolios =
                folioRepository.saveAll(folios);

        folioRepository.flush();

        // 10. Lưu tồn kho mới.
        fnbMenuRepository.saveAll(lockedMenus);

        // 11. Trả về Folio ID.
        List<PlaceFnbOrderResponse.ItemResult> finalItems =
                new ArrayList<>();

        for (int i = 0; i < savedFolios.size(); i++) {

            PlaceFnbOrderResponse.ItemResult item =
                    itemResults.get(i);

            finalItems.add(
                    new PlaceFnbOrderResponse.ItemResult(
                            item.menuId(),
                            item.menuName(),
                            item.quantity(),
                            item.unitPrice(),
                            item.amount(),
                            item.remainingQuantity(),
                            savedFolios.get(i).getFolioId()
                    )
            );
        }

        return new PlaceFnbOrderResponse(
                reservation.getReservationCode(),
                guest.getGuestId(),
                request.serviceTime(),
                finalItems,
                totalAmount,
                "Đặt dịch vụ F&B thành công"
        );
    }

    private void validateRequest(
            PlaceFnbOrderRequest request
    ) {

        if (request == null
                || request.items() == null
                || request.items().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Danh sách món không được để trống"
            );
        }

        if (request.serviceTime() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thời gian phục vụ là bắt buộc"
            );
        }

        if (request.items().stream().anyMatch(
                item -> item == null
                        || item.menuId() == null
                        || item.quantity() == null
                        || item.quantity() <= 0
        )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thông tin món hoặc số lượng không hợp lệ"
            );
        }
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

    private String createServiceData(
            FnbMenu menu,
            java.time.LocalDateTime serviceTime
    ) {

        Map<String, Object> data = new LinkedHashMap<>();

        data.put("menuId", menu.getFnbMenuId());
        data.put("menuName", menu.getName());
        data.put("serviceTime", serviceTime);

        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Không thể tạo dữ liệu Folio",
                    exception
            );
        }
    }
}