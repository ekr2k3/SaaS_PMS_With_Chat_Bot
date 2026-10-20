
package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPayment;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentGateway;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.enums.PaymentStatus;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionPaymentRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;
import com.ekr.saas_pms_with_chatBot.config.StripeProperties;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Service
public class AdminSubscriptionPaymentService {




    private final SubscriptionRepository subscriptionRepository;
    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final StripeService stripeService;
    private final StripeProperties stripeProperties;
    private final MailService mailService;

    public AdminSubscriptionPaymentService(
            SubscriptionRepository subscriptionRepository,
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            SubscriptionPaymentRepository paymentRepository,
            StripeService stripeService,
            StripeProperties stripeProperties,
            MailService mailService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.paymentRepository = paymentRepository;
        this.stripeService = stripeService;
        this.stripeProperties = stripeProperties;
        this.mailService = mailService;
    }


    /**
     * Owner retry payment cho lần đăng ký Subscription ban đầu.
     * Không dùng cho late/early renewal; các luồng đó có API riêng.
     */
    @Transactional(dontRollbackOn = ResponseStatusException.class)
    public Map<String, Object> retryInitialSubscriptionPayment(String ownerEmail) {
        Account owner = accountRepository.findByEmail(ownerEmail);
        if (owner == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy tài khoản Owner"
            );
        }

        Tenant tenant = tenantRepository.findByAccount(owner)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Tài khoản chưa có Tenant"
                ));

        Subscription subscription = subscriptionRepository.findByTenant(tenant)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Tenant chưa có Subscription"
                ));

        if (subscription.getStatus()
                != Subscription.SubscriptionStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chỉ Subscription ở trạng thái PENDING_PAYMENT mới được retry thanh toán"
            );
        }

        SubscriptionPlan plan = subscription.getPlan();
        if (plan == null || plan.getStatus() != SubscriptionPlan.PlanStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Subscription Plan hiện không hoạt động"
            );
        }

        if (owner.getEmail() == null || owner.getEmail().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Owner chưa có email hợp lệ"
            );
        }

        BigDecimal amount = plan.getPrice();
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Giá Subscription phải lớn hơn 0"
            );
        }

        String currency = stripeProperties.getCurrency().toUpperCase();
        long stripeAmount = toStripeAmount(amount, currency);

        SubscriptionPayment latestPayment = paymentRepository
                .findFirstBySubscription_SubscriptionIdAndPaymentGatewayOrderByIdDesc(
                        subscription.getSubscriptionId(),
                        PaymentGateway.STRIPE
                )
                .orElse(null);

        if (latestPayment != null
                && latestPayment.getStatus() == PaymentStatus.SUCCEEDED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Subscription đã có Payment thành công"
            );
        }

        if (latestPayment != null) {
            Map<String, Object> latestMetadata = latestPayment.getMetadata();
            if (latestMetadata != null
                    && (latestMetadata.containsKey("renewalPlanId")
                    || latestMetadata.containsKey("earlyRenewalPlanId"))) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Payment gần nhất thuộc luồng gia hạn; không thể retry bằng API đăng ký ban đầu"
                );
            }
        }

        // Nếu Session vẫn đang mở, gửi lại chính URL đó thay vì tạo checkout trùng.
        if (latestPayment != null
                && latestPayment.getStatus() == PaymentStatus.PENDING) {
            String sessionId = getMetadataString(latestPayment, "checkoutSessionId");
            String checkoutUrl = getMetadataString(latestPayment, "checkoutUrl");

            if (sessionId != null && !sessionId.isBlank()) {
                try {
                    Session existingSession = stripeService.retrieveCheckoutSession(sessionId);
                    String sessionStatus = existingSession.getStatus();

                    String activeCheckoutUrl =
                            checkoutUrl != null && !checkoutUrl.isBlank()
                                    ? checkoutUrl
                                    : existingSession.getUrl();

                    if ("open".equalsIgnoreCase(sessionStatus)
                            && activeCheckoutUrl != null
                            && !activeCheckoutUrl.isBlank()) {
                        try {
                            mailService.sendPaymentEmail(
                                    owner.getEmail(), activeCheckoutUrl,
                                    latestPayment.getAmount(),
                                    latestPayment.getCurrency(), plan.getName()
                            );
                        } catch (RuntimeException mailException) {
                            throw new ResponseStatusException(
                                    HttpStatus.BAD_GATEWAY,
                                    "Checkout vẫn còn hiệu lực nhưng không gửi được email thanh toán",
                                    mailException
                            );
                        }
                        return Map.of(
                                "message", "Checkout hiện tại vẫn còn hiệu lực; đã gửi lại email thanh toán",
                                "subscriptionId", subscription.getSubscriptionId(),
                                "paymentId", latestPayment.getId(),
                                "checkoutUrl", activeCheckoutUrl,
                                "reusedCheckoutSession", true
                        );
                    }

                    if ("complete".equalsIgnoreCase(sessionStatus)) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Checkout đã hoàn tất; vui lòng chờ hệ thống xác nhận thanh toán"
                        );
                    }

                    if ("expired".equalsIgnoreCase(sessionStatus)) {
                        latestPayment.setStatus(PaymentStatus.CANCELLED);
                        paymentRepository.saveAndFlush(latestPayment);
                    } else {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Không thể xác định trạng thái Checkout hiện tại"
                        );
                    }
                } catch (StripeException exception) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "Không thể kiểm tra Checkout Session hiện tại trên Stripe",
                            exception
                    );
                }
            } else if (checkoutUrl != null && !checkoutUrl.isBlank()) {
                // URL tồn tại nhưng thiếu Session ID: không tạo phiên thứ hai không an toàn.
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Payment thiếu Checkout Session ID; cần kiểm tra dữ liệu thanh toán"
                );
            }
        }

        SubscriptionPayment paymentToRetry;
        boolean createNewPayment = latestPayment == null
                || latestPayment.getStatus() == PaymentStatus.FAILED
                || latestPayment.getStatus() == PaymentStatus.CANCELLED;

        if (latestPayment != null
                && latestPayment.getStatus() == PaymentStatus.PROCESSING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Thanh toán đang được xử lý; không thể tạo phiên mới"
            );
        }

        if (latestPayment != null
                && latestPayment.getStatus() != PaymentStatus.PENDING
                && latestPayment.getStatus() != PaymentStatus.FAILED
                && latestPayment.getStatus() != PaymentStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Trạng thái Payment hiện tại không cho phép retry"
            );
        }

        if (createNewPayment) {
            paymentToRetry = new SubscriptionPayment();
            paymentToRetry.setSubscription(subscription);
            paymentToRetry.setPaymentGateway(PaymentGateway.STRIPE);
            paymentToRetry.setStatus(PaymentStatus.PENDING);
            paymentToRetry.setAmount(amount);
            paymentToRetry.setCurrency(currency);
            paymentToRetry.setOrderId("SUB-" + UUID.randomUUID());
            paymentToRetry.setRequestId("stripe-sub-" + UUID.randomUUID());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("planId", plan.getPlanId());
            metadata.put("planName", plan.getName());
            metadata.put("ownerEmail", owner.getEmail());
            if (latestPayment != null) {
                metadata.put("retryOfPaymentId", latestPayment.getId());
            }
            paymentToRetry.setMetadata(metadata);
            paymentToRetry = paymentRepository.saveAndFlush(paymentToRetry);
        } else {
            // Nếu Stripe call lần trước lỗi trước khi lưu Session, tái sử dụng
            // Payment và requestId để Stripe idempotency trả lại cùng kết quả.
            paymentToRetry = latestPayment;
        }

        try {
            Session session = stripeService.createCheckoutSession(
                    paymentToRetry.getId(),
                    paymentToRetry.getOrderId(),
                    paymentToRetry.getRequestId(),
                    plan.getName(),
                    stripeAmount,
                    currency,
                    subscription.getSubscriptionId(),
                    plan.getPlanId()
            );

            if (session.getId() == null
                    || session.getUrl() == null
                    || session.getUrl().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Stripe không trả về Checkout Session URL hợp lệ"
                );
            }

            Map<String, Object> metadata = paymentToRetry.getMetadata() == null
                    ? new HashMap<>()
                    : new HashMap<>(paymentToRetry.getMetadata());
            metadata.put("checkoutSessionId", session.getId());
            metadata.put("checkoutUrl", session.getUrl());
            paymentToRetry.setMetadata(metadata);
            paymentRepository.saveAndFlush(paymentToRetry);

            try {
                mailService.sendPaymentEmail(
                        owner.getEmail(), session.getUrl(), amount, currency, plan.getName()
                );
            } catch (RuntimeException mailException) {
                // Giữ Payment + Checkout URL đã lưu. Owner có thể gọi lại API
                // để hệ thống gửi lại email mà không tạo Checkout Session khác.
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Đã tạo Checkout Session nhưng không gửi được email thanh toán; hãy retry để gửi lại email",
                        mailException
                );
            }

            return Map.of(
                    "message", "Đã tạo phiên thanh toán mới và gửi email cho Owner",
                    "subscriptionId", subscription.getSubscriptionId(),
                    "paymentId", paymentToRetry.getId(),
                    "checkoutUrl", session.getUrl(),
                    "reusedCheckoutSession", false
            );
        } catch (StripeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể tạo Stripe Checkout Session. Có thể retry lại bằng API này.",
                    exception
            );
        }
    }

    @Transactional
    public void approveAndCreatePayment(Long subscriptionId) {

        // 1. Tìm Subscription
        Subscription subscription = subscriptionRepository
                .findById(subscriptionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Subscription"
                ));

        // 2. Chỉ xử lý yêu cầu đang chờ duyệt.
        // Retry Payment có endpoint riêng của Owner.
        Subscription.SubscriptionStatus currentStatus =
                subscription.getStatus();

        boolean waitingForApproval =
                currentStatus == Subscription.SubscriptionStatus.PENDING_APPROVAL
                        || currentStatus == Subscription.SubscriptionStatus.PENDING;

        if (!waitingForApproval) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Subscription không ở trạng thái có thể khởi tạo thanh toán"
            );
        }

        // 3. Kiểm tra Plan, Tenant, Owner
        SubscriptionPlan plan = subscription.getPlan();
        Tenant tenant = subscription.getTenant();

        if (plan == null || tenant == null || tenant.getAccount() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Subscription thiếu Plan, Tenant hoặc Owner"
            );
        }

        if (plan.getStatus() != SubscriptionPlan.PlanStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Subscription Plan hiện không hoạt động"
            );
        }

        Account owner = tenant.getAccount();

        if (owner.getEmail() == null || owner.getEmail().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Owner chưa có email hợp lệ"
            );
        }

        BigDecimal amount = plan.getPrice();

        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Giá Subscription phải lớn hơn 0"
            );
        }

        String currency =
                stripeProperties.getCurrency().toUpperCase();

        long stripeAmount = toStripeAmount(amount, currency);

        // 4. Tìm payment hiện có của Subscription.
        // Không tạo bản ghi mới nếu đây là lần retry.
        SubscriptionPayment payment = paymentRepository
                .findFirstBySubscription_SubscriptionIdAndPaymentGatewayOrderByIdDesc(
                        subscriptionId,
                        PaymentGateway.STRIPE
                )
                .orElse(null);

        if (payment != null) {

            // Đã có URL: dùng lại và gửi lại email.
            String existingUrl = getMetadataString(
                    payment,
                    "checkoutUrl"
            );

            if (existingUrl != null && !existingUrl.isBlank()) {
                mailService.sendPaymentEmail(
                        owner.getEmail(),
                        existingUrl,
                        payment.getAmount(),
                        payment.getCurrency(),
                        plan.getName()
                );

                subscription.setStatus(
                        Subscription.SubscriptionStatus.PENDING_PAYMENT
                );
                subscriptionRepository.save(subscription);
                return;
            }

            // Nếu đã tồn tại payment nhưng không thể retry an toàn,
            // không âm thầm tạo thêm payment mới.
            if (payment.getStatus() != PaymentStatus.PENDING) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Payment hiện tại không còn ở trạng thái PENDING"
                );
            }

            // Không thay requestId/orderId khi retry.
        } else {

            // 5. Đánh dấu Subscription đã được duyệt
            subscription.setStatus(
                    Subscription.SubscriptionStatus.PENDING_PAYMENT
            );
            subscriptionRepository.save(subscription);

            // 6. Tạo Payment record trước khi gọi Stripe
            payment = new SubscriptionPayment();

            payment.setSubscription(subscription);
            payment.setPaymentGateway(PaymentGateway.STRIPE);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setAmount(amount);
            payment.setCurrency(currency);

            payment.setOrderId("SUB-" + UUID.randomUUID());
            payment.setRequestId("stripe-sub-" + UUID.randomUUID());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("planId", plan.getPlanId());
            metadata.put("planName", plan.getName());
            metadata.put("ownerEmail", owner.getEmail());

            payment.setMetadata(metadata);



            payment = paymentRepository.saveAndFlush(payment);
        }

        // 7. Nếu đã có URL thì đã xử lý ở trên.
        // Nếu chưa có URL, gọi Stripe bằng requestId đã lưu.
        try {
            Session session = stripeService.createCheckoutSession(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getRequestId(),
                    plan.getName(),
                    stripeAmount,
                    currency,
                    subscriptionId,
                    plan.getPlanId()
            );

            if (session.getId() == null
                    || session.getUrl() == null
                    || session.getUrl().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Stripe không trả về Checkout Session URL hợp lệ"
                );
            }

            // 8. Lưu Checkout Session ID và URL.
            // Không ghi session.getId() vào transactionId.
            Map<String, Object> metadata = payment.getMetadata();

            if (metadata == null) {
                metadata = new HashMap<>();
            } else {
                metadata = new HashMap<>(metadata);
            }

            metadata.put("checkoutSessionId", session.getId());
            metadata.put("checkoutUrl", session.getUrl());

            payment.setMetadata(metadata);
            paymentRepository.saveAndFlush(payment);

            // 9. Gửi email cho Owner
            mailService.sendPaymentEmail(
                    owner.getEmail(),
                    session.getUrl(),
                    amount,
                    currency,
                    plan.getName()
            );

        } catch (StripeException e) {
            // Giữ payment PENDING và requestId để retry cùng idempotency key.
            // Không tạo payment mới chỉ vì lần gọi Stripe bị lỗi.
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể tạo hoặc xác nhận Stripe Checkout Session. "
                            + "Hãy retry bằng cùng payment record.",
                    e
            );
        }
    }

    private String getMetadataString(
            SubscriptionPayment payment,
            String key
    ) {
        Map<String, Object> metadata = payment.getMetadata();

        if (metadata == null) {
            return null;
        }

        Object value = metadata.get(key);
        return value == null ? null : value.toString();
    }

    private long toStripeAmount(
            BigDecimal amount,
            String currency
    ) {
        // VND là đồng tiền không dùng phần thập phân khi gửi Stripe.
        if ("VND".equals(currency)
                || "JPY".equals(currency)
                || "KRW".equals(currency)) {

            return amount
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .longValueExact();
        }

        // Ví dụ USD: 10.00 USD -> 1000 cents.
        return amount
                .movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }

    @Transactional
    public void rejectSubscription(Long subscriptionId) {

        Subscription subscription = subscriptionRepository
                .findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Subscription"
                ));

        if (subscription.getStatus()
                != Subscription.SubscriptionStatus.PENDING_APPROVAL
                && subscription.getStatus()
                != Subscription.SubscriptionStatus.PENDING) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chỉ được từ chối Subscription đang chờ phê duyệt"
            );
        }

        Tenant tenant = subscription.getTenant();

        if (tenant == null || tenant.getAccount() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không tìm thấy thông tin Owner của Subscription"
            );
        }

        Account owner = tenant.getAccount();

        if (owner.getEmail() == null || owner.getEmail().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Owner chưa có email hợp lệ"
            );
        }

        // 1. Cập nhật trạng thái từ chối.
        subscription.setStatus(
                Subscription.SubscriptionStatus.REJECTED
        );

        subscriptionRepository.saveAndFlush(subscription);

        // 2. Gửi email cho Owner.
        // Lỗi email không làm mất kết quả từ chối đã xử lý.
        try {
            mailService.sendSubscriptionRejectedEmail(
                    owner.getEmail(),
                    owner.getName(),
                    tenant.getName(),
                    subscription.getSubscriptionId(),
                    subscription.getPlan().getName()
            );
        } catch (Exception e) {
            System.out.println(
                    "Không gửi được email từ chối Subscription ID " + subscriptionId + "tới " + owner.getEmail() + e
            );
        }
    }

}