package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.property.UpdateProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class UpdatePropertyRequest {

    @Size(max = 150, message = "Ten Property toi da 150 ky tu")
    private String name;

    @Size(max = 500, message = "Dia chi toi da 500 ky tu")
    private String address;

    @Size(max = 30, message = "So dien thoai toi da 30 ky tu")
    private String phone;

    @Email(message = "Email khong dung dinh dang")
    @Size(max = 255, message = "Email toi da 255 ky tu")
    private String email;

    private String description;

    private MultipartFile image;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }
}
