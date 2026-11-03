package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.Tool;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RoomType;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.enums.RoomTypeName;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomTypeRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.onlinereservation.SearchAvailableRooms.SearchAvailableRoomsService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

@Component
public class RoomAvailabilityTool {

    private final SearchAvailableRoomsService availabilityService;
    private final RoomTypeRepository roomTypeRepository;

    public RoomAvailabilityTool(
            SearchAvailableRoomsService availabilityService,
            RoomTypeRepository roomTypeRepository) {
        this.availabilityService = availabilityService;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Tool(description = """
            Kiểm tra số phòng còn khả dụng của một Room Type trong Property hiện tại.
            Chỉ gọi khi người dùng hỏi về phòng trống/đặt phòng theo ngày.
            Bắt buộc có roomTypeName, checkInDate, checkOutDate và quantity.
            propertyId luôn được lấy từ ToolContext do Backend truyền vào; tuyệt đối
            không lấy propertyId từ câu hỏi hoặc tự chọn Property khác.
            Kết quả success=false nghĩa là không kiểm tra được, không được suy đoán số phòng.
            """)
    public Map<String, Object> checkRoomAvailability(
            @ToolParam(description = "Tên Room Type theo enum: STANDARD, SUPERIOR, DELUXE, EXECUTIVE, SUITE, FAMILY, PRESIDENTIAL")
            String roomTypeName,
            @ToolParam(description = "Ngày nhận phòng theo định dạng yyyy-MM-dd")
            LocalDate checkInDate,
            @ToolParam(description = "Ngày trả phòng theo định dạng yyyy-MM-dd")
            LocalDate checkOutDate,
            @ToolParam(description = "Số lượng phòng khách muốn đặt, phải lớn hơn 0")
            Integer quantity,
            ToolContext toolContext) {

        Long propertyId = resolvePropertyId(toolContext);
        if (propertyId == null || propertyId <= 0) {
            throw new IllegalStateException(
                    "Room Availability Tool không có Property context hợp lệ");
        }

        if (roomTypeName == null || roomTypeName.isBlank()) {
            return error("INVALID_ROOM_TYPE",
                    "Tên loại phòng không được để trống.");
        }

        if (checkInDate == null || checkOutDate == null
                || !checkOutDate.isAfter(checkInDate)) {
            return error("INVALID_DATE_RANGE",
                    "Ngày nhận phòng và ngày trả phòng không hợp lệ. Ngày trả phòng phải sau ngày nhận phòng.");
        }

        if (quantity == null || quantity <= 0) {
            return error("INVALID_QUANTITY",
                    "Số lượng phòng phải lớn hơn 0.");
        }

        final RoomTypeName parsedName;
        try {
            parsedName = RoomTypeName.valueOf(
                    roomTypeName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return error("INVALID_ROOM_TYPE",
                    "Loại phòng không hợp lệ: " + roomTypeName
                            + ". Các loại phòng được hỗ trợ: STANDARD, SUPERIOR, DELUXE, EXECUTIVE, SUITE, FAMILY, PRESIDENTIAL.");
        }

        RoomType roomType = roomTypeRepository
                .findByProperty_PropertyIdAndName(propertyId, parsedName)
                .orElse(null);

        if (roomType == null) {
            return error("ROOM_TYPE_NOT_FOUND",
                    "Không tìm thấy loại phòng " + parsedName
                            + " trong Property hiện tại.");
        }

        try {
            int availableQuantity = availabilityService.countAvailableRooms(
                    propertyId,
                    checkInDate,
                    checkOutDate,
                    roomType.getRoomTypeId());

            return Map.of(
                    "success", true,
                    "roomTypeName", parsedName.name(),
                    "checkInDate", checkInDate.toString(),
                    "checkOutDate", checkOutDate.toString(),
                    "requestedQuantity", quantity,
                    "availableQuantity", availableQuantity,
                    "available", availableQuantity >= quantity
            );
        } catch (ResponseStatusException exception) {
            return error(errorCode(exception.getStatusCode().value()),
                    exception.getReason() == null
                            ? "Không thể kiểm tra tình trạng phòng ở thời điểm hiện tại."
                            : exception.getReason());
        }
    }

    private Long resolvePropertyId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }

        Object rawPropertyId = toolContext.getContext().get("propertyId");
        if (!(rawPropertyId instanceof Number number)) {
            return null;
        }

        return number.longValue();
    }

    private Map<String, Object> error(String errorCode, String message) {
        return Map.of(
                "success", false,
                "errorCode", errorCode,
                "message", message
        );
    }

    private String errorCode(int statusCode) {
        if (statusCode == HttpStatus.BAD_REQUEST.value()) {
            return "INVALID_SEARCH_CRITERIA";
        }
        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            return "PROPERTY_NOT_FOUND";
        }
        if (statusCode == HttpStatus.FORBIDDEN.value()) {
            return "PROPERTY_UNAVAILABLE";
        }
        return "AVAILABILITY_CHECK_FAILED";
    }
}