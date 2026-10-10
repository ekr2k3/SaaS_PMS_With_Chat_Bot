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
                "[Hotel SaaS] Có yêu cầu Subscription mới cần xét duyệt";

        String body = """
                Kính gửi SaaS Admin,

                Hệ thống vừa nhận được một yêu cầu đăng ký Subscription mới.

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
            BigDecimal amount
    ) {

        SimpleMailMessage message =
                new SimpleMailMessage();


        message.setTo(ownerEmail);

        message.setSubject(
                "Thanh toán Subscription"
        );


        String content =
                "Xin chào,\n\n"

                        + "Yêu cầu đăng ký Subscription "
                        + "của bạn đã được SaaS Admin "
                        + "phê duyệt.\n\n"

                        + "Số tiền cần thanh toán: "
                        + amount
                        + " VND\n\n"

                        + "Vui lòng truy cập liên kết "
                        + "bên dưới để thực hiện thanh toán:\n\n"

                        + payUrl

                        + "\n\n"
                        + "Trân trọng,\n"
                        + "SaaS PMS";


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

}
