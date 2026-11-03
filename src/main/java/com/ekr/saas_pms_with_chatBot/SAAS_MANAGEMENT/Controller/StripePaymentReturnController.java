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
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">
                    <title>Thanh toán</title>
                </head>
                <body>
                    <h2>Đã quay lại từ Stripe</h2>

                    <p>
                        Yêu cầu thanh toán đã được gửi về hệ thống.
                    </p>

                    <p>
                        Trạng thái giao dịch đang được xác nhận.
                        Vui lòng quay lại ứng dụng để kiểm tra kết quả.
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
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">
                    <title>Thanh toán</title>
                </head>
                <body>
                    <h2>Bạn đã rời trang thanh toán</h2>

                    <p>
                        Giao dịch chưa được xác nhận thành công.
                    </p>

                    <p>
                        Vui lòng quay lại ứng dụng để kiểm tra trạng thái
                        hoặc thực hiện thanh toán lại nếu Payment Attempt
                        vẫn còn hiệu lực.
                    </p>
                </body>
                </html>
                """;
    }
}