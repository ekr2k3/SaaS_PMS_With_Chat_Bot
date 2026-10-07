package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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
}
