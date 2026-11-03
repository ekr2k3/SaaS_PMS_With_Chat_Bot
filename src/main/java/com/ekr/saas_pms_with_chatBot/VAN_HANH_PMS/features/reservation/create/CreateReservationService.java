package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.create;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.*;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.*;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.*;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.*;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.confirmation.ReservationConfirmedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class CreateReservationService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DEPOSIT_POLICY = "DEPOSIT";
    private static final String PERCENTAGE_DEPOSIT =
            "PERCENTAGE_DEPOSIT";
    private static final String FIXED_AMOUNT_DEPOSIT =
            "FIXED_AMOUNT_DEPOSIT";

    private static final String CURRENCY = "VND";

    private static final int HOLD_MINUTES = 15;

    private static final List<ReservationStatus> BLOCKING_STATUSES =
            List.of(
                    ReservationStatus.CONFIRMED,
                    ReservationStatus.CHECKED_IN
            );

    private final PropertyRepository propertyRepository;
    private final RoomRepository roomRepository;
    private final RoomRateRepository roomRateRepository;

    private final ReservationRepository reservationRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final ReservationGuestRepository reservationGuestRepository;
    private final ReservationRoomRateSnapshotRepository snapshotRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final GuestRepository guestRepository;

    private final AppliedPmsRepository appliedPmsRepository;
    private final PolicyFormulaParameterRepository parameterRepository;

    private final ObjectMapper objectMapper;
    private final CreateReservationValidator validator;

    public CreateReservationService(
            PropertyRepository propertyRepository,
            RoomRepository roomRepository,
            RoomRateRepository roomRateRepository,
            ReservationRepository reservationRepository,
            ReservationRoomRepository reservationRoomRepository,
            ReservationGuestRepository reservationGuestRepository,
            ReservationRoomRateSnapshotRepository snapshotRepository,
            GuestRepository guestRepository,
            AppliedPmsRepository appliedPmsRepository,
            PolicyFormulaParameterRepository parameterRepository,
            ObjectMapper objectMapper,
            CreateReservationValidator validator,
            ApplicationEventPublisher eventPublisher
    ) {
        this.propertyRepository = propertyRepository;
        this.roomRepository = roomRepository;
        this.roomRateRepository = roomRateRepository;
        this.reservationRepository = reservationRepository;
        this.reservationRoomRepository = reservationRoomRepository;
        this.reservationGuestRepository = reservationGuestRepository;
        this.snapshotRepository = snapshotRepository;
        this.guestRepository = guestRepository;
        this.appliedPmsRepository = appliedPmsRepository;
        this.parameterRepository = parameterRepository;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CreateReservationResponse create(
            Long propertyId,
            CreateReservationRequest request
    ) {

        validator.validate(request);

        LocalDateTime now = LocalDateTime.now();

        // 1. Kiểm tra Property và trạng thái Subscription.
        Property property = getBookableProperty(propertyId);

        // 2. Lock Room theo room_id tăng dần.
        List<SelectedRoom> selectedRooms =
                lockAndValidateRooms(propertyId, request, now);

        // 3. Kiểm tra tổng sức chứa.
        validateGuestCapacity(
                request.guestCount(),
                selectedRooms
        );

        // 4. Tính tổng tiền từ Room Rate trong database.
        long numberOfNights = ChronoUnit.DAYS.between(
                request.checkInDate(),
                request.checkOutDate()
        );

        BigDecimal totalAmount = calculateTotalAmount(
                selectedRooms,
                numberOfNights
        );

        // 5. Tìm hoặc tạo Guest.
        Guest guest = findOrCreateGuest(request.guest());

        // 6. Tính Deposit theo Policy Formula của Property.
        DepositCalculation depositCalculation =
                calculateDeposit(propertyId, totalAmount);

        BigDecimal depositAmount =
                depositCalculation.depositAmount();

        boolean requiresPayment =
                depositAmount.compareTo(BigDecimal.ZERO) > 0;

        // 7. Tạo Reservation.
        Reservation reservation = new Reservation();

        reservation.setReservationCode(generateReservationCode());
        reservation.setCheckInDate(request.checkInDate());
        reservation.setCheckOutDate(request.checkOutDate());
        reservation.setGuestCount(request.guestCount());
        reservation.setProperty(property);
        reservation.setTotalAmount(totalAmount);
        reservation.setDepositAmount(depositAmount);
        reservation.setCurrency(CURRENCY);

        reservation.setStatus(
                requiresPayment
                        ? ReservationStatus.PENDING_PAYMENT
                        : ReservationStatus.CONFIRMED
        );

        reservation.setPaymentExpiresAt(
                requiresPayment
                        ? now.plusMinutes(HOLD_MINUTES)
                        : null
        );

        reservation.setDepositFormulaSnapshot(
                depositCalculation.formulaSnapshot()
        );

        reservation = reservationRepository.save(reservation);

        // 8. Lưu quan hệ Reservation - Room.
        List<ReservationRoom> reservationRooms =
                new ArrayList<>();

        for (SelectedRoom selectedRoom : selectedRooms) {

            ReservationRoom reservationRoom =
                    new ReservationRoom();

            reservationRoom.setReservationId(
                    reservation.getReservationId()
            );

            reservationRoom.setRoomId(
                    selectedRoom.room().getRoomId()
            );

            reservationRooms.add(reservationRoom);
        }

        reservationRooms =
                reservationRoomRepository.saveAll(reservationRooms);

        Map<Long, ReservationRoom> reservationRoomMap =
                new HashMap<>();

        for (ReservationRoom reservationRoom : reservationRooms) {
            reservationRoomMap.put(
                    reservationRoom.getRoomId(),
                    reservationRoom
            );
        }

        // 9. Lưu Reservation - Guest.
        ReservationGuest reservationGuest =
                new ReservationGuest();

        reservationGuest.setReservationId(
                reservation.getReservationId()
        );

        reservationGuest.setGuestId(guest.getGuestId());

        reservationGuestRepository.save(reservationGuest);

        // 10. Lưu giá từng phòng cho từng đêm.
        saveRoomRateSnapshots(
                reservation,
                selectedRooms,
                reservationRoomMap,
                request.checkInDate(),
                request.checkOutDate()
        );

        // 11. Reservation không cần thanh toán Deposit
// được xác nhận ngay khi tạo.
        if (!requiresPayment) {

            eventPublisher.publishEvent(
                    new ReservationConfirmedEvent(
                            reservation.getReservationId()
                    )
            );
        }

        // 12. Trả kết quả.
        return buildResponse(
                reservation,
                selectedRooms,
                numberOfNights
        );
    }

    /**
     * Property chỉ nhận Reservation khi:
     * - Tenant ACTIVE.
     * - Subscription ACTIVE.
     * - Subscription còn hiệu lực theo ngày.
     * - Plan ACTIVE.
     */
    private Property getBookableProperty(Long propertyId) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Property not found"
                ));

        Tenant tenant = property.getTenant();

        if (tenant == null
                || tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Property is not available for reservation"
            );
        }

        Subscription subscription = tenant.getSubscription();

        LocalDate today = LocalDate.now();

        if (subscription == null
                || subscription.getStatus()
                != Subscription.SubscriptionStatus.ACTIVE
                || subscription.getStartDate() == null
                || subscription.getEndDate() == null
                || subscription.getStartDate().isAfter(today)
                || subscription.getEndDate().isBefore(today)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Property subscription is not active"
            );
        }

        SubscriptionPlan plan = subscription.getPlan();

        if (plan == null
                || plan.getStatus()
                != SubscriptionPlan.PlanStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Property subscription plan is inactive"
            );
        }

        return property;
    }

    /**
     * Khóa tất cả phòng theo thứ tự tăng dần để giảm nguy cơ deadlock.
     * Sau khi lock phải kiểm tra lại toàn bộ điều kiện.
     */
    private List<SelectedRoom> lockAndValidateRooms(
            Long propertyId,
            CreateReservationRequest request,
            LocalDateTime now
    ) {

        List<Long> roomIds = request.rooms()
                .stream()
                .map(CreateReservationRequest.RoomSelection::roomId)
                .sorted()
                .toList();

        Map<Long, CreateReservationRequest.RoomSelection> selectionMap =
                new HashMap<>();

        for (CreateReservationRequest.RoomSelection selection
                : request.rooms()) {

            selectionMap.put(selection.roomId(), selection);
        }

        List<SelectedRoom> selectedRooms = new ArrayList<>();

        for (Long roomId : roomIds) {

            Room room = roomRepository
                    .findByRoomIdAndPropertyIdForUpdate(
                            roomId,
                            propertyId
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Room " + roomId
                                    + " does not belong to this Property"
                    ));

            RoomType roomType = room.getRoomType();

            if (!ACTIVE.equalsIgnoreCase(roomType.getStatus())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Room Type is inactive: " + roomType.getRoomTypeId()
                );
            }

            if (!Boolean.TRUE.equals(room.getIsAvailable())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Room is not available: " + roomId
                );
            }

            // Kiểm tra Reservation trùng ngày sau khi đã lock.
            long conflicts =
                    reservationRepository.countConflictingReservations(
                            roomId,
                            propertyId,
                            request.checkInDate(),
                            request.checkOutDate(),
                            BLOCKING_STATUSES,
                            ReservationStatus.PENDING_PAYMENT,
                            now
                    );

            if (conflicts > 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Room is already reserved for the selected dates: "
                                + roomId
                );
            }

            // Xác minh Room Rate.
            Long roomRateId = selectionMap.get(roomId).roomRateId();

            RoomRate roomRate = roomRateRepository
                    .findByRoomRateIdAndRoomType_Property_PropertyId(
                            roomRateId,
                            propertyId
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Room Rate not found: " + roomRateId
                    ));

            if (!ACTIVE.equalsIgnoreCase(roomRate.getStatus())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Room Rate is inactive: " + roomRateId
                );
            }

            if (!Objects.equals(
                    roomRate.getRoomType().getRoomTypeId(),
                    roomType.getRoomTypeId()
            )) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Room Rate does not belong to selected Room Type"
                );
            }

            selectedRooms.add(
                    new SelectedRoom(room, roomRate)
            );
        }

        return selectedRooms;
    }

    private void validateGuestCapacity(
            Integer guestCount,
            List<SelectedRoom> selectedRooms
    ) {

        int totalCapacity = selectedRooms.stream()
                .mapToInt(item -> item.room().getRoomType().getCapacity())
                .sum();

        if (guestCount > totalCapacity) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Guest count exceeds the selected rooms' capacity"
            );
        }
    }

    private BigDecimal calculateTotalAmount(
            List<SelectedRoom> selectedRooms,
            long numberOfNights
    ) {

        BigDecimal total = BigDecimal.ZERO;

        for (SelectedRoom selectedRoom : selectedRooms) {

            BigDecimal roomTotal =
                    selectedRoom.rate().getPrice()
                            .multiply(BigDecimal.valueOf(numberOfNights));

            selectedRoom.setTotalPrice(roomTotal);

            total = total.add(roomTotal);
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Quy tắc tái sử dụng Guest:
     * - Khớp Email hoặc Phone.
     * - Nếu Email và Phone trỏ đến hai Guest khác nhau thì từ chối.
     * - Không bao giờ gộp Guest chỉ dựa trên Full Name.
     */
    private Guest findOrCreateGuest(
            CreateReservationRequest.GuestInformation information
    ) {

        String email = normalizeEmail(information.email());
        String phone = normalizePhone(information.phone());

        Set<Guest> matches = new LinkedHashSet<>();

        if (email != null) {
            matches.addAll(
                    guestRepository.findAllByEmailIgnoreCase(email)
            );
        }

        if (phone != null) {
            matches.addAll(
                    guestRepository.findAllByPhone(phone)
            );
        }

        if (matches.size() > 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Guest email and phone belong to different records"
            );
        }

        if (matches.size() == 1) {

            Guest existing = matches.iterator().next();

            if (existing.getEmail() != null
                    && email != null
                    && !existing.getEmail().equalsIgnoreCase(email)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Guest email does not match existing record"
                );
            }

            if (existing.getPhone() != null
                    && phone != null
                    && !existing.getPhone().equals(phone)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Guest phone does not match existing record"
                );
            }

            // Chỉ bổ sung thông tin liên hệ còn thiếu.
            if (existing.getEmail() == null && email != null) {
                existing.setEmail(email);
            }

            if (existing.getPhone() == null && phone != null) {
                existing.setPhone(phone);
            }

            return guestRepository.save(existing);
        }

        Guest guest = new Guest();

        guest.setFullName(information.fullName().trim());
        guest.setEmail(email);
        guest.setPhone(phone);
        guest.setIdentityNumber(information.identityNumber());
        guest.setIdentityType(information.identityType());
        guest.setDateOfBirth(information.dateOfBirth());
        guest.setGender(information.gender());
        guest.setNationality(information.nationality());
        guest.setAddress(information.address());

        return guestRepository.save(guest);
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {

        if (phone == null || phone.isBlank()) {
            return null;
        }

        return phone.trim();
    }

    /**
     * Đọc Deposit Formula được áp dụng cho Property.
     *
     * Nếu không có công thức DEPOSIT:
     * depositAmount = 0.
     *
     * Nếu có công thức:
     * - PERCENTAGE_DEPOSIT
     * - FIXED_AMOUNT_DEPOSIT
     */
    private DepositCalculation calculateDeposit(
            Long propertyId,
            BigDecimal totalAmount
    ) {

        List<AppliedPms> appliedPolicies =
                appliedPmsRepository
                        .findAllByPropertyIdOrderByPolicyFormulaIdAsc(
                                propertyId
                        );

        List<PolicyFormula> depositFormulas =
                appliedPolicies.stream()
                        .map(AppliedPms::getPolicyFormula)
                        .filter(Objects::nonNull)
                        .filter(formula ->
                                DEPOSIT_POLICY.equalsIgnoreCase(
                                        formula.getPolicyType()
                                )
                        )
                        .toList();

        if (depositFormulas.isEmpty()) {
            return new DepositCalculation(
                    BigDecimal.ZERO,
                    null
            );
        }

        if (depositFormulas.size() > 1) {
            throw new IllegalStateException(
                    "Property has more than one Deposit Formula"
            );
        }

        PolicyFormula formula = depositFormulas.get(0);

        if (!ACTIVE.equalsIgnoreCase(formula.getStatus())) {
            throw new IllegalStateException(
                    "Applied Deposit Formula is inactive"
            );
        }

        List<PolicyFormulaParameter> parameters =
                parameterRepository
                        .findAllByPropertyIdAndPolicyFormulaId(
                                propertyId,
                                formula.getPolicyFormulaId()
                        );

        Map<String, String> parameterValues = new LinkedHashMap<>();

        for (PolicyFormulaParameter parameter : parameters) {
            parameterValues.put(
                    parameter.getParamName(),
                    parameter.getParamValue()
            );
        }

        JsonNode definition;

        try {
            definition = objectMapper.readTree(
                    formula.getFormulaDefinition()
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Invalid Deposit Formula definition",
                    exception
            );
        }

        String kind = definition.path("kind").asText();

        BigDecimal depositAmount;

        if (PERCENTAGE_DEPOSIT.equals(kind)) {

            String rawPercentage =
                    parameterValues.get("depositPercentage");

            BigDecimal percentage =
                    parseRequiredDecimal(
                            rawPercentage,
                            "depositPercentage"
                    );

            if (percentage.compareTo(BigDecimal.ZERO) <= 0
                    || percentage.compareTo(
                            new BigDecimal("100")
                    ) > 0) {

                throw new IllegalStateException(
                        "depositPercentage must be greater than 0 "
                                + "and not greater than 100"
                );
            }

            depositAmount = totalAmount
                    .multiply(percentage)
                    .divide(
                            new BigDecimal("100"),
                            2,
                            RoundingMode.HALF_UP
                    );

        } else if (FIXED_AMOUNT_DEPOSIT.equals(kind)) {

            String rawAmount =
                    parameterValues.get("depositAmount");

            BigDecimal fixedAmount =
                    parseRequiredDecimal(
                            rawAmount,
                            "depositAmount"
                    );

            if (fixedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException(
                        "depositAmount must be greater than 0"
                );
            }

            // Không yêu cầu khách đặt cọc nhiều hơn tổng tiền phòng.
            depositAmount = fixedAmount.min(totalAmount);

        } else {

            throw new IllegalStateException(
                    "Unsupported Deposit Formula: " + kind
            );
        }

        depositAmount = depositAmount.setScale(
                2,
                RoundingMode.HALF_UP
        );

        String snapshot = createFormulaSnapshot(
                formula,
                kind,
                parameterValues,
                depositAmount
        );

        return new DepositCalculation(
                depositAmount,
                snapshot
        );
    }

    private BigDecimal parseRequiredDecimal(
            String value,
            String parameterName
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing Deposit Formula parameter: "
                            + parameterName
            );
        }

        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Invalid Deposit Formula parameter: "
                            + parameterName,
                    exception
            );
        }
    }

    private String createFormulaSnapshot(
            PolicyFormula formula,
            String kind,
            Map<String, String> parameters,
            BigDecimal calculatedDeposit
    ) {

        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put(
                "policyFormulaId",
                formula.getPolicyFormulaId()
        );

        snapshot.put("policyType", formula.getPolicyType());
        snapshot.put("formulaName", formula.getName());
        snapshot.put("kind", kind);
        snapshot.put("parameters", parameters);
        snapshot.put("calculatedDeposit", calculatedDeposit);
        snapshot.put("currency", CURRENCY);

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Cannot create Deposit Formula snapshot",
                    exception
            );
        }
    }

    /**
     * Tạo giá snapshot cho từng đêm:
     * Check-in inclusive, Check-out exclusive.
     */
    private void saveRoomRateSnapshots(
            Reservation reservation,
            List<SelectedRoom> selectedRooms,
            Map<Long, ReservationRoom> reservationRoomMap,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {

        List<ReservationRoomRateSnapshot> snapshots =
                new ArrayList<>();

        for (SelectedRoom selectedRoom : selectedRooms) {

            Room room = selectedRoom.room();
            RoomRate rate = selectedRoom.rate();

            LocalDate stayDate = checkInDate;

            while (stayDate.isBefore(checkOutDate)) {

                ReservationRoomRateSnapshot snapshot =
                        new ReservationRoomRateSnapshot();

                snapshot.setReservationId(
                        reservation.getReservationId()
                );

                snapshot.setRoomId(room.getRoomId());

                snapshot.setRoomRateId(rate.getRoomRateId());

                snapshot.setRateName(rate.getRateName().name());

                snapshot.setStayDate(stayDate);

                snapshot.setPrice(rate.getPrice());

                snapshot.setReservationRoom(
                        reservationRoomMap.get(room.getRoomId())
                );

                snapshots.add(snapshot);

                stayDate = stayDate.plusDays(1);
            }
        }

        snapshotRepository.saveAll(snapshots);
    }

    private String generateReservationCode() {

        String code;

        do {
            code = "RSV-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 16)
                    .toUpperCase(Locale.ROOT);

        } while (reservationRepository.existsByReservationCode(code));

        return code;
    }

    private CreateReservationResponse buildResponse(
            Reservation reservation,
            List<SelectedRoom> selectedRooms,
            long numberOfNights
    ) {

        List<CreateReservationResponse.RoomPriceDetail> roomDetails =
                selectedRooms.stream()
                        .map(item ->
                                new CreateReservationResponse.RoomPriceDetail(
                                        item.room().getRoomId(),
                                        item.room().getRoomNumber(),
                                        item.rate().getRoomRateId(),
                                        item.rate().getRateName().name(),
                                        item.rate().getPrice(),
                                        item.totalPrice()
                                )
                        )
                        .toList();

        return new CreateReservationResponse(
                reservation.getReservationId(),
                reservation.getReservationCode(),
                reservation.getStatus(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                Math.toIntExact(numberOfNights),
                reservation.getGuestCount(),
                reservation.getTotalAmount(),
                reservation.getDepositAmount(),
                reservation.getCurrency(),
                reservation.getPaymentExpiresAt(),
                roomDetails
        );
    }

    private record DepositCalculation(
            BigDecimal depositAmount,
            String formulaSnapshot
    ) {
    }

    private static class SelectedRoom {

        private final Room room;
        private final RoomRate rate;
        private BigDecimal totalPrice;

        private SelectedRoom(
                Room room,
                RoomRate rate
        ) {
            this.room = room;
            this.rate = rate;
        }

        public Room room() {
            return room;
        }

        public RoomRate rate() {
            return rate;
        }

        public BigDecimal totalPrice() {
            return totalPrice;
        }

        public void setTotalPrice(BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
        }
    }
}
