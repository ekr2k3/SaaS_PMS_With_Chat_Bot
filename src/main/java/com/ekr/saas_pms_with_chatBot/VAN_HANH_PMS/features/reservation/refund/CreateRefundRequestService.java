package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationPayment;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationPaymentRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.RefundAccessService;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refundquote.GetRefundQuoteService;
import com.stripe.model.Refund;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.transaction.annotation.Propagation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class CreateRefundRequestService {
    private static final Logger log =
            LoggerFactory.getLogger(CreateRefundRequestService.class);
    private final ReservationRepository reservations;
    private final ReservationPaymentRepository payments;
    private final ReservationStripeRefundGateway gateway;
    private final RefundAccessService access;
    private final GetRefundQuoteService quoteService;
    private final ApplicationEventPublisher eventPublisher;
    private final NoShowRefundPolicyService noShowRefundPolicyService;

    public CreateRefundRequestService(ReservationRepository reservations, ReservationPaymentRepository payments,
            ReservationStripeRefundGateway gateway, RefundAccessService access, GetRefundQuoteService quoteService,
            ApplicationEventPublisher eventPublisher, NoShowRefundPolicyService noShowRefundPolicyService) {
        this.reservations=reservations; this.payments=payments; this.gateway=gateway; this.access=access;
        this.quoteService=quoteService; this.eventPublisher=eventPublisher;
        this.noShowRefundPolicyService=noShowRefundPolicyService;
    }

    @Transactional
    public CreateRefundRequestResponse createForReceptionist(Long propertyId,String reservationCode,CreateRefundRequestRequest request) {
        access.requirePermission(propertyId,"REFUND_CREATE");
        Reservation reservation=reservations.findByReservationCodeForUpdate(reservationCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Reservation not found"));
        validateCancelledAndProperty(reservation,propertyId);
        ReservationPayment original=payments.findByIdForUpdate(request.reservationPaymentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Payment not found"));
        if(!reservation.getReservationId().equals(original.getReservation().getReservationId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Payment not found");
        if (quoteService.getInternalQuote(reservation).refundableAmount().signum() <= 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Cancellation policy does not allow a refund");
        String cancellationReason = request.reason() != null && !request.reason().isBlank()
                ? request.reason().trim() : original.getNote();
        return createOne(reservation,original,cancellationReason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createAutomaticRefundsForReservationId(
            Long reservationId,
            String cancellationReason
    ) {
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reservation not found"
                ));

        createAutomaticRefunds(reservation, cancellationReason);
    }

    @Transactional
    public void createAutomaticRefunds(Reservation reservation, String cancellationReason) {
        if(reservation.getStatus()!=ReservationStatus.CANCELLED) return;
        if(quoteService.getInternalQuote(reservation).refundableAmount().signum() <= 0) return;
        List<ReservationPayment> originals=payments.findAllByReservationIdForUpdate(reservation.getReservationId()).stream()
                .filter(p -> (p.getPaymentType()==ReservationPaymentType.DEPOSIT || p.getPaymentType()==ReservationPaymentType.BALANCE)
                        && (p.getStatus()==ReservationPaymentStatus.SUCCEEDED || p.getStatus()==ReservationPaymentStatus.REFUNDED)).toList();
        for(ReservationPayment original:originals) {
            BigDecimal alreadyRefunded=payments.findAllByTransactionIdAndPaymentType(originalTransactionId(original),ReservationPaymentType.REFUND).stream()
                    .filter(p -> p.getStatus()==ReservationPaymentStatus.SUCCEEDED || p.getStatus()==ReservationPaymentStatus.PROCESSING || p.getStatus()==ReservationPaymentStatus.PENDING)
                    .map(ReservationPayment::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
            BigDecimal amount=original.getAmount().subtract(alreadyRefunded).max(BigDecimal.ZERO);
            if(amount.signum()>0) {
                String note = cancellationReason != null && !cancellationReason.isBlank()
                        ? cancellationReason.trim() : original.getNote();
                createOne(reservation,original,note,amount);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNoShowRefundsForReservationId(
            Long reservationId
    ) {

        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reservation not found"
                ));

        if (reservation.getStatus() != ReservationStatus.NO_SHOW) {
            return;
        }

        List<ReservationPayment> allPayments =
                payments.findAllByReservationIdForUpdate(
                        reservation.getReservationId()
                );

        List<NoShowRefundAllocation> allocations =
                noShowRefundPolicyService.calculate(
                        reservation,
                        allPayments
                );

        for (NoShowRefundAllocation allocation : allocations) {

            ReservationPayment original =
                    allocation.originalPayment();

            BigDecimal amount =
                    allocation.refundAmount();

            if (amount.signum() <= 0) {
                continue;
            }

            if (original.getStatus() != ReservationPaymentStatus.SUCCEEDED
                    && original.getStatus() != ReservationPaymentStatus.REFUNDED) {
                continue;
            }

            String transactionId = originalTransactionId(original);

            if (transactionId == null || transactionId.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Original Payment has no Stripe transaction identifier"
                );
            }

            createOne(
                    reservation,
                    original,
                    "NO_SHOW",
                    amount
            );
        }
    }

    private CreateRefundRequestResponse createOne(Reservation reservation,ReservationPayment original,String reason) {
        BigDecimal alreadyRefunded=payments.findAllByTransactionIdAndPaymentType(originalTransactionId(original),ReservationPaymentType.REFUND).stream()
                .filter(p -> p.getStatus()==ReservationPaymentStatus.SUCCEEDED || p.getStatus()==ReservationPaymentStatus.PROCESSING || p.getStatus()==ReservationPaymentStatus.PENDING)
                .map(ReservationPayment::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal remaining=original.getAmount().subtract(alreadyRefunded).max(BigDecimal.ZERO);
        return createOne(reservation,original,reason,remaining);
    }

    private CreateRefundRequestResponse createOne(Reservation reservation,ReservationPayment original,String reason,BigDecimal amount) {
        if(original.getPaymentType()==ReservationPaymentType.REFUND || (original.getStatus()!=ReservationPaymentStatus.SUCCEEDED && original.getStatus()!=ReservationPaymentStatus.REFUNDED))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Only successful original payments can be refunded");
        if(amount.signum()<=0) throw new ResponseStatusException(HttpStatus.CONFLICT,"No refundable amount remains");
        ReservationPayment refundPayment=new ReservationPayment();
        refundPayment.setReservation(reservation);
        refundPayment.setPaymentType(ReservationPaymentType.REFUND);
        refundPayment.setAmount(amount);
        refundPayment.setCurrency(original.getCurrency()!=null?original.getCurrency():reservation.getCurrency());
        refundPayment.setPaymentMethod("STRIPE");
        refundPayment.setNote(reason);
        refundPayment.setTransactionId(originalTransactionId(original));
        refundPayment.setStatus(ReservationPaymentStatus.PROCESSING);
        payments.saveAndFlush(refundPayment);
        try {

            log.info("========== STRIPE REFUND REQUEST ==========");
            log.info("Reservation ID: {}", reservation.getReservationId());
            log.info("Original Payment ID: {}", original.getReservationPaymentId());
            log.info("Refund Payment ID: {}", refundPayment.getReservationPaymentId());
            log.info("Sending refund request to Stripe...");

            Refund stripeRefund = gateway.createRefund(
                    original,
                    refundPayment,
                    reason
            );

// 1. Chứng minh Stripe API đã trả về đối tượng Refund
            log.info("========== STRIPE REFUND API RESPONSE ==========");
            log.info("Stripe Refund ID: {}", stripeRefund.getId());
            log.info("Stripe Refund Status: {}", stripeRefund.getStatus());
            log.info("Stripe Refund Amount: {}", stripeRefund.getAmount());
            log.info("Stripe Refund Currency: {}", stripeRefund.getCurrency());
            log.info("Stripe Payment Intent: {}", stripeRefund.getPaymentIntent());
            log.info("Stripe Failure Reason: {}", stripeRefund.getFailureReason());
            log.info("Stripe Created Timestamp: {}", stripeRefund.getCreated());

// 2. Backend cập nhật Refund Payment
            refundPayment.setGatewayRefundId(stripeRefund.getId());

            String status = stripeRefund.getStatus();

            refundPayment.setStatus(
                    "succeeded".equalsIgnoreCase(status)
                            ? ReservationPaymentStatus.SUCCEEDED
                            : "failed".equalsIgnoreCase(status)
                              ? ReservationPaymentStatus.FAILED
                              : ReservationPaymentStatus.PROCESSING
            );

            log.info("========== BACKEND REFUND STATUS MAPPING ==========");
            log.info("Stripe Status: {}", status);
            log.info("Backend ReservationPayment Status: {}", refundPayment.getStatus());
            log.info("==============================================");


            if(refundPayment.getStatus()==ReservationPaymentStatus.SUCCEEDED) {
                original.setStatus(ReservationPaymentStatus.REFUNDED);
                refundPayment.setPaidAt(java.time.LocalDateTime.now());
                payments.save(original);
            }
            payments.save(refundPayment);
            if (refundPayment.getStatus() == ReservationPaymentStatus.SUCCEEDED
                    || refundPayment.getStatus() == ReservationPaymentStatus.FAILED) {
                eventPublisher.publishEvent(new RefundStatusChangedEvent(
                        refundPayment.getReservationPaymentId(), refundPayment.getStatus()));
            }
        } catch (Exception ex) {

        log.error("========== STRIPE REFUND API FAILED ==========");
        log.error("Reservation ID: {}", reservation.getReservationId());
        log.error("Original Payment ID: {}", original.getReservationPaymentId());
        log.error("Refund Payment ID: {}", refundPayment.getReservationPaymentId());
        log.error("Exception Type: {}", ex.getClass().getName());
        log.error("Exception Message: {}", ex.getMessage(), ex);

        // Không kết luận thất bại vì request có thể đã tới Stripe.
        // Giữ PROCESSING để chờ kết quả xác minh từ Webhook.
        refundPayment.setStatus(ReservationPaymentStatus.PROCESSING);
        payments.save(refundPayment);

        log.warn(
                "Refund Payment {} remains PROCESSING. Waiting for Stripe webhook.",
                refundPayment.getReservationPaymentId()
        );
    }
        return new CreateRefundRequestResponse(refundPayment.getReservationPaymentId(),original.getReservationPaymentId(),
                reservation.getReservationCode(),amount,refundPayment.getCurrency(),refundPayment.getStatus().name(),refundPayment.getGatewayRefundId());
    }
    private String originalTransactionId(ReservationPayment original) {
        return original.getPaymentIntentId()!=null?original.getPaymentIntentId():original.getTransactionId();
    }

    private void validateCancelledAndProperty(Reservation r,Long propertyId) {
        if(r.getProperty()==null || !propertyId.equals(r.getProperty().getPropertyId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Reservation not found");
        if(r.getStatus()!=ReservationStatus.CANCELLED) throw new ResponseStatusException(HttpStatus.CONFLICT,"Reservation must be cancelled before refund");
    }
}
