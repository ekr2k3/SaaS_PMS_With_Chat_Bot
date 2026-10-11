package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/stripe")
public class StripePaymentReturnController {

    @GetMapping(
            value = "/success",
            produces = MediaType.TEXT_HTML_VALUE
    )
    public String success(
            @RequestParam(
                    value = "session_id",
                    required = false
            ) String sessionId
    ) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <title>Thanh toán</title>
                </head>
                <body>
                    <h2>Đã quay lại từ Stripe</h2>
                    <p>
                        Hệ thống đang xác nhận trạng thái thanh toán.
                        Vui lòng kiểm tra trạng thái Subscription.
                    </p>
                </body>
                </html>
                """;
    }

    @GetMapping(
            value = "/cancel",
            produces = MediaType.TEXT_HTML_VALUE
    )
    public String cancel() {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <title>Hủy thanh toán</title>
                </head>
                <body>
                    <h2>Bạn đã rời trang thanh toán</h2>
                    <p>
                        Trạng thái thanh toán sẽ được hệ thống
                        cập nhật theo kết quả Stripe gửi về.
                    </p>
                </body>
                </html>
                """;
    }
}