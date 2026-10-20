package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${acc.user}")
    private String saasAdminEmail;


    public void sendSubscriptionRequestNotification(
            String accountEmail,
            String tenantName,
            Long tenantId,
            Long subscriptionId,
            String planName,
            String tenantStatus,
            String subscriptionStatus
    ) {

        String subject =
                "[Hotel SaaS] Có yêu cầu Subscription cần xét duyệt";

        String body = """
                Kính gửi SaaS Admin,

                Hệ thống vừa nhận được yêu cầu đăng ký mới hoặc gửi lại Subscription cần xét duyệt.

                Thông tin yêu cầu:

                - Account: %s
                - Tên tổ chức: %s
                - Tenant ID: %d
                - Subscription ID: %d
                - Gói đăng ký: %s
                - Trạng thái Tenant: %s
                - Trạng thái Subscription: %s

                Vui lòng đăng nhập hệ thống quản trị để kiểm tra
                và xét duyệt yêu cầu Subscription.

                Trân trọng,
                Hotel SaaS System
                """.formatted(
                accountEmail,
                tenantName,
                tenantId,
                subscriptionId,
                planName,
                tenantStatus,
                subscriptionStatus
        );

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(saasAdminEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }



    // Gửi payURL cho Owner

    public void sendPaymentEmail(
            String ownerEmail,
            String payUrl,
            BigDecimal amount,
            String currency,
            String planName
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(ownerEmail);
        message.setSubject("[Hotel SaaS] Thanh toán Subscription");

        String content = """
            Xin chào,

            Yêu cầu đăng ký Subscription của bạn đã được SaaS Admin phê duyệt.

            Gói đăng ký: %s
            Số tiền cần thanh toán: %s %s

            Vui lòng mở đường dẫn dưới đây để thanh toán:
            %s

            Lưu ý: Subscription chỉ được kích hoạt sau khi hệ thống
            xác nhận thanh toán thành công.

            Trân trọng,
            SaaS PMS
            """.formatted(
                planName,
                amount.toPlainString(),
                currency,
                payUrl
        );

        message.setText(content);
        mailSender.send(message);
    }

    // =========================
// PAYMENT SUCCESS EMAIL
// =========================

    public void sendPaymentSuccessEmail(
            String ownerEmail,
            String orderId,
            BigDecimal amount
    ) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(ownerEmail);

        message.setSubject(
                "[Hotel SaaS] Thanh toán Subscription thành công"
        );

        String content =
                "Xin chào,\n\n"

                        + "Thanh toán Subscription của bạn "
                        + "đã được thực hiện thành công.\n\n"

                        + "Mã đơn hàng: "
                        + orderId
                        + "\n\n"

                        + "Số tiền: "
                        + amount
                        + " VND\n\n"

                        + "Subscription của bạn đã được kích hoạt.\n"

                        + "Tenant của bạn hiện đã có thể sử dụng hệ thống.\n\n"

                        + "Trân trọng,\n"
                        + "SaaS PMS";

        message.setText(content);

        mailSender.send(message);
    }


    // =========================
// PAYMENT FAILED EMAIL
// =========================

    public void sendPaymentFailedEmail(
            String ownerEmail,
            String orderId,
            BigDecimal amount,
            String reason
    ) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(ownerEmail);

        message.setSubject(
                "[Hotel SaaS] Thanh toán Subscription thất bại"
        );

        String content =
                "Xin chào,\n\n"

                        + "Thanh toán Subscription của bạn "
                        + "không thành công.\n\n"

                        + "Mã đơn hàng: "
                        + orderId
                        + "\n\n"

                        + "Số tiền: "
                        + amount
                        + " VND\n\n"

                        + "Lý do: "
                        + reason
                        + "\n\n"

                        + "Subscription chưa được kích hoạt.\n"

                        + "Vui lòng thực hiện thanh toán lại "
                        + "hoặc liên hệ SaaS Admin.\n\n"

                        + "Trân trọng,\n"
                        + "SaaS PMS";

        message.setText(content);

        mailSender.send(message);
    }

    public void sendSubscriptionRenewalReminder(
            String ownerEmail,
            String ownerName,
            String tenantName,
            Long subscriptionId,
            java.time.LocalDate endDate
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(ownerEmail);
        message.setSubject(
                "[Hotel SaaS] Nhắc gia hạn Subscription"
        );

        String content = """
            Xin chào %s,

            Đây là email nhắc gia hạn Subscription của tổ chức bạn.

            Thông tin Subscription:
            - Tổ chức: %s
            - Subscription ID: %d
            - Ngày hết hạn hiện tại: %s

            Vui lòng đăng nhập hệ thống để kiểm tra thông tin
            gói dịch vụ và thực hiện gia hạn nếu cần.

            Email này chỉ nhằm mục đích nhắc nhở.
            Hệ thống không tự động thanh toán hoặc gia hạn
            Subscription khi gửi email này.

            Trân trọng,
            SaaS PMS
            """.formatted(
                ownerName,
                tenantName,
                subscriptionId,
                endDate
        );

        message.setText(content);

        // Nếu SMTP/provider ném exception, để service nghiệp vụ
        // xử lý lỗi của Subscription hiện tại.
        mailSender.send(message);
    }


    public void sendSubscriptionExpiredEmail(
            String ownerEmail,
            String ownerName,
            String tenantName,
            Long subscriptionId
    ) {

        if (ownerEmail == null || ownerEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Email Owner không hợp lệ"
            );
        }

        String safeOwnerName =
                ownerName == null || ownerName.isBlank()
                        ? "Quý khách"
                        : ownerName;

        String safeTenantName =
                tenantName == null || tenantName.isBlank()
                        ? "Tổ chức của bạn"
                        : tenantName;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(ownerEmail);

        message.setSubject(
                "[Hotel SaaS] Subscription hết hạn - PMS đã bị vô hiệu hóa"
        );

        String content = """
            Xin chào %s,

            Subscription của tổ chức bạn đã hết hạn và chưa có
            thanh toán thành công cho kỳ gia hạn hiện tại.

            Thông tin:
            - Tổ chức: %s
            - Subscription ID: %d
            - Trạng thái Subscription: EXPIRED
            - Trạng thái Tenant: INACTIVE

            Quyền sử dụng các chức năng PMS của tổ chức đã bị
            vô hiệu hóa.

            Tài khoản Owner vẫn có thể đăng nhập để xem thông tin
            và thực hiện gia hạn theo chức năng hệ thống cung cấp.

            Sau khi thanh toán gia hạn thành công và được hệ thống
            xác nhận, quyền sử dụng PMS sẽ được kích hoạt lại theo
            chính sách của hệ thống.

            Trân trọng,
            SaaS PMS
            """.formatted(
                safeOwnerName,
                safeTenantName,
                subscriptionId
        );

        message.setText(content);

        mailSender.send(message);
    }

    // EMAIL THÔNG BÁO TỪ CHỐI SUBSCRIPTION
    public void sendSubscriptionRejectedEmail(
            String ownerEmail,
            String ownerName,
            String tenantName,
            Long subscriptionId,
            String planName
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(ownerEmail);
        message.setSubject(
                "[Hotel SaaS] Yêu cầu đăng ký Subscription bị từ chối"
        );

        String content = """
            Xin chào %s,

            Rất tiếc, yêu cầu đăng ký Subscription của bạn
            đã bị SaaS Admin từ chối.

            Thông tin yêu cầu:
            - Tổ chức: %s
            - Subscription ID: %d
            - Gói đăng ký: %s
            - Trạng thái: REJECTED

            Bạn vui lòng liên hệ bộ phận hỗ trợ của Hotel SaaS
            để biết thêm thông tin nếu cần.

            Trân trọng,
            SaaS PMS
            """.formatted(
                ownerName,
                tenantName,
                subscriptionId,
                planName
        );

        message.setText(content);
        mailSender.send(message);
    }
}