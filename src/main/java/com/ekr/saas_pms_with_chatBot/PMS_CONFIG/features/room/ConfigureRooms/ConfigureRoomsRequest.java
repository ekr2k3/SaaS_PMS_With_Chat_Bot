package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.room.ConfigureRooms;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ConfigureRoomsRequest {

    @NotNull(message = "Danh sach Room khong duoc null")
    @Valid
    private List<RoomItem> rooms;

    public ConfigureRoomsRequest() {
    }

    public List<RoomItem> getRooms() {
        return rooms;
    }

    public void setRooms(List<RoomItem> rooms) {
        this.rooms = rooms;
    }

    public static class RoomItem {

        // null: tạo Room mới; có giá trị: cập nhật Room hiện tại.
        private Long roomId;

        @NotBlank(message = "Room number khong duoc de trong")
        private String roomNumber;

        @NotNull(message = "Floor khong duoc null")
        @Min(value = 1, message = "Floor phai lon hon 0")
        private Integer floor;

        @NotBlank(message = "Status khong duoc de trong")
        private String status;

        private String description;

        private Boolean isAvailable = true;

        @NotNull(message = "Room type ID khong duoc null")
        private Long roomTypeId;

        public RoomItem() {
        }

        public Long getRoomId() {
            return roomId;
        }

        public void setRoomId(Long roomId) {
            this.roomId = roomId;
        }

        public String getRoomNumber() {
            return roomNumber;
        }

        public void setRoomNumber(String roomNumber) {
            this.roomNumber = roomNumber;
        }

        public Integer getFloor() {
            return floor;
        }

        public void setFloor(Integer floor) {
            this.floor = floor;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Boolean getIsAvailable() {
            return isAvailable;
        }

        public void setIsAvailable(Boolean isAvailable) {
            this.isAvailable = isAvailable;
        }

        public Long getRoomTypeId() {
            return roomTypeId;
        }

        public void setRoomTypeId(Long roomTypeId) {
            this.roomTypeId = roomTypeId;
        }
    }
}
