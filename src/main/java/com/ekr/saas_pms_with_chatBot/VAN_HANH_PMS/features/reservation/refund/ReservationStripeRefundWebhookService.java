package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationPayment;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationPaymentRepository;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReservationStripeRefundWebhookService {
    private final ReservationPaymentRepository payments;
    private final ApplicationEventPublisher eventPublisher;
    public ReservationStripeRefundWebhookService(ReservationPaymentRepository payments,
            ApplicationEventPublisher eventPublisher){
        this.payments=payments;
        this.eventPublisher=eventPublisher;
    }

    @Transactional
    public void handleVerifiedPropertyEvent(Event event,Long expectedPropertyId){
        if(event==null || expectedPropertyId==null) throw new IllegalArgumentException("Invalid verified refund event");
        if(!"refund.created".equals(event.getType()) && !"refund.updated".equals(event.getType())) return;
        Optional<StripeObject> object=event.getDataObjectDeserializer().getObject();
        if(object.isEmpty() || !(object.get() instanceof Refund refund))
            throw new IllegalArgumentException("Cannot deserialize Stripe Refund event");
        Map<String,String> metadata=refund.getMetadata();
        if(metadata==null || !"RESERVATION_REFUND".equals(metadata.get("payment_flow"))) return;
        if(!String.valueOf(expectedPropertyId).equals(metadata.get("property_id")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Refund event Property mismatch");
        String refundPaymentId=metadata.get("refund_payment_id");
        if(!StringUtils.hasText(refundPaymentId)) throw new IllegalArgumentException("Missing refund_payment_id metadata");
        Long id;
        try{id=Long.valueOf(refundPaymentId);}catch(NumberFormatException ex){throw new IllegalArgumentException("Invalid refund_payment_id metadata",ex);}
        ReservationPayment refundPayment=payments.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Refund payment not found"));
        if(refundPayment.getPaymentType()!=ReservationPaymentType.REFUND || refundPayment.getReservation()==null ||
                refundPayment.getReservation().getProperty()==null ||
                !expectedPropertyId.equals(refundPayment.getReservation().getProperty().getPropertyId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Refund payment does not belong to verified Property");
        if(StringUtils.hasText(refundPayment.getGatewayRefundId()) && !refundPayment.getGatewayRefundId().equals(refund.getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Stripe Refund ID mismatch");
        refundPayment.setGatewayRefundId(refund.getId());
        ReservationPaymentStatus previousStatus=refundPayment.getStatus();

        if(previousStatus==ReservationPaymentStatus.SUCCEEDED || previousStatus==ReservationPaymentStatus.FAILED) {
            payments.save(refundPayment);
            return;
        }

        String status=refund.getStatus();
        if("succeeded".equalsIgnoreCase(status)){
            refundPayment.setStatus(ReservationPaymentStatus.SUCCEEDED);
            refundPayment.setPaidAt(java.time.LocalDateTime.now());
            List<ReservationPayment> originals=payments.findAllByTransactionIdAndPaymentType(
                    refundPayment.getTransactionId(), ReservationPaymentType.DEPOSIT);
            if(originals.isEmpty()) originals=payments.findAllByTransactionIdAndPaymentType(
                    refundPayment.getTransactionId(), ReservationPaymentType.BALANCE);
            originals.forEach(p -> {
                p.setStatus(ReservationPaymentStatus.REFUNDED);
            });
            payments.saveAll(originals);
        } else if("failed".equalsIgnoreCase(status) || "canceled".equalsIgnoreCase(status)){
            refundPayment.setStatus(ReservationPaymentStatus.FAILED);
        } else {
            refundPayment.setStatus(ReservationPaymentStatus.PROCESSING);
        }
        payments.save(refundPayment);

        if(refundPayment.getStatus()!=previousStatus
                && (refundPayment.getStatus()==ReservationPaymentStatus.SUCCEEDED
                || refundPayment.getStatus()==ReservationPaymentStatus.FAILED)) {
            eventPublisher.publishEvent(new RefundStatusChangedEvent(
                    refundPayment.getReservationPaymentId(),refundPayment.getStatus()));
        }
    }
}
