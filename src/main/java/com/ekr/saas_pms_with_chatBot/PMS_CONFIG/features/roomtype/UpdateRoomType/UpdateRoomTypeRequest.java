
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.roomtype.UpdateRoomType;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.enums.RoomTypeName;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UpdateRoomTypeRequest {

    @NotNull(message = "Tên Room Type không được để trống")
    private RoomTypeName name;

    private String description;

    @NotNull(message = "Sức chứa không được để trống")
    @Min(value = 1, message = "Sức chứa phải lớn hơn 0")
    private Integer capacity;

    @NotNull(message = "Loại giường không được để trống")
    @Size(
            min = 1,
            max = 50,
            message = "Loại giường phải có tối đa 50 ký tự"
    )
    private String bedType;

    @NotNull(message = "Số giường không được để trống")
    @Min(value = 1, message = "Số giường phải lớn hơn 0")
    private Integer numberOfBeds;

    @NotNull(message = "Số phòng không được để trống")
    @Min(value = 0, message = "Số phòng không được âm")
    private Integer numberOfRooms;

    @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự")
    private String image;

    private List<@Size(
            max = 100,
            message = "Mỗi tiện ích tối đa 100 ký tự"
    ) String> amenities;
}