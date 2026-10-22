package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.property.CreateProperty;



import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.infrastructure.storage.PropertyImageStorageService;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreatePropertyService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyImageStorageService imageStorageService;

    public CreatePropertyService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            SubscriptionRepository subscriptionRepository,
            PropertyRepository propertyRepository,
            PropertyImageStorageService imageStorageService
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.propertyRepository = propertyRepository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional
    public CreatePropertyResponse execute(
            String ownerEmail,
            CreatePropertyRequest request,
            MultipartFile image
    ) {
        // 1. Xác định Account từ người đang đăng nhập.
        Account account = accountRepository.findByEmail(ownerEmail);

        if (account == null
                || account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản không hợp lệ hoặc đã bị vô hiệu hóa"
            );
        }

        // 2. Xác định Tenant của Owner.
        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tài khoản chưa được liên kết với Tenant"
                ));

        if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tenant hiện không được phép tạo Property"
            );
        }

        // 3. Khóa Subscription trong transaction để tuần tự hóa
        // các yêu cầu tạo Property của cùng Tenant.
        Subscription subscription =
                subscriptionRepository
                        .findByTenantIdForUpdate(tenant.getTenantId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Tenant chưa có Subscription"
                        ));

        if (subscription.getStatus()
                != Subscription.SubscriptionStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Subscription chưa ACTIVE"
            );
        }

        SubscriptionPlan plan = subscription.getPlan();

        if (plan.getStatus()
                != SubscriptionPlan.PlanStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Plan hiện không hoạt động"
            );
        }

        // 4. Kiểm tra hạn mức Property của Plan.
        long currentCount =
                propertyRepository.countByTenant_TenantId(
                        tenant.getTenantId()
                );

        int maxProperties = plan.getMaxProperties();

        if (currentCount >= maxProperties) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tenant đã đạt giới hạn "
                            + maxProperties
                            + " Property của Plan"
            );
        }

        // 5. Upload ảnh nếu người dùng có cung cấp.
        String uploadedImagePath = null;

        try {
            if (image != null && !image.isEmpty()) {
                uploadedImagePath = imageStorageService.upload(
                        tenant.getTenantId(),
                        image
                );
            }

            // 6. Tạo Entity theo cấu trúc bảng PROPERTY hiện có.
            Property property = Property.builder()
                    .name(request.getName().trim())
                    .address(request.getAddress().trim())
                    .phone(normalizeOptionalText(request.getPhone()))
                    .email(normalizeOptionalText(request.getEmail()))
                    .description(
                            normalizeOptionalText(request.getDescription())
                    )
                    .image(uploadedImagePath)
                    .tenant(tenant)
                    .build();

            // Flush để phát hiện lỗi lưu database ngay trong khối try.
            Property savedProperty =
                    propertyRepository.saveAndFlush(property);

            // 7. Trả dữ liệu Property vừa tạo.
            return new CreatePropertyResponse(
                    savedProperty.getPropertyId(),
                    savedProperty.getName(),
                    savedProperty.getAddress(),
                    savedProperty.getPhone(),
                    savedProperty.getEmail(),
                    savedProperty.getDescription(),
                    savedProperty.getImage()
            );

        } catch (RuntimeException ex) {
            // Database rollback không tự xóa object đã upload lên MinIO.
            // Cố gắng dọn ảnh nếu việc tạo Property thất bại.
            if (uploadedImagePath != null) {
                try {
                    imageStorageService.delete(uploadedImagePath);
                } catch (RuntimeException cleanupException) {
                    ex.addSuppressed(cleanupException);
                }
            }

            throw ex;
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
