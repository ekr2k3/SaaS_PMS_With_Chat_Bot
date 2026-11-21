package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.cancel;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationGuest;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationPayment;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationGuestRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationPaymentRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.ReservationRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.RefundAccessService;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.get.GetReservationValidator;
import org.springframework.context.ApplicationEventPublisher;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refundquote.GetRefundQuoteService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class CancelReservationService {
    private final ReservationRepository reservations;
    private final ReservationGuestRepository guests;
    private final ReservationPaymentRepository payments;
    private final RefundAccessService access;
    private final GetReservationValidator validator;
    private final GetRefundQuoteService quoteService;
    private final ApplicationEventPublisher eventPublisher;

    public CancelReservationService(ReservationRepository reservations,ReservationGuestRepository guests,
            ReservationPaymentRepository payments, RefundAccessService access,GetReservationValidator validator,
            GetRefundQuoteService quoteService, ApplicationEventPublisher eventPublisher){
        this.reservations=reservations;this.guests=guests;this.payments=payments;this.access=access;this.validator=validator;
        this.quoteService=quoteService;this.eventPublisher=eventPublisher;
    }

    @Transactional
    public CancelReservationResponse cancelByGuest(String code,CancelReservationRequest request){
        validator.validate(code,request.email(),request.phone());
        Reservation r=reservations.findByReservationCodeForUpdate(code.trim()).orElseThrow(this::notFound);
        verifyGuest(r,request.email(),request.phone());
        var quote=quoteService.getInternalQuote(r);
        cancel(r);
        persistCancellationReasonOnPayments(r, request.reason());
        // Initiate automatic Refund only after the cancellation transaction commits.
        eventPublisher.publishEvent(new ReservationCancelledEvent(r.getReservationId(), true, request.reason()));
        return new CancelReservationResponse(r.getReservationCode(),r.getStatus().name(),quote.refundableAmount(),r.getCurrency(),
                quote.refundableAmount().signum()>0?"PROCESSING":"NOT_APPLICABLE","Reservation cancelled successfully");
    }

    @Transactional
    public CancelReservationResponse cancelByReceptionist(Long propertyId,String code,CancelReservationRequest request){
        access.requirePermission(propertyId,"RESERVATION_CANCEL");
        validator.validate(code,request.email(),request.phone());
        Reservation r=reservations.findByReservationCodeForUpdate(code.trim()).orElseThrow(this::notFound);
        if(r.getProperty()==null || !propertyId.equals(r.getProperty().getPropertyId())) throw notFound();
        verifyGuest(r,request.email(),request.phone());
        var quote=quoteService.getInternalQuote(r);
        cancel(r);
        persistCancellationReasonOnPayments(r, request.reason());
        // Receptionist explicitly creates the Refund through F21 after cancellation.
        eventPublisher.publishEvent(new ReservationCancelledEvent(r.getReservationId(), false, request.reason()));
        return new CancelReservationResponse(r.getReservationCode(),r.getStatus().name(),quote.refundableAmount(),r.getCurrency(),
                quote.refundableAmount().signum()>0?"PENDING":"NOT_APPLICABLE","Reservation cancelled successfully");
    }

    private void cancel(Reservation r){
        if(r.getStatus()==ReservationStatus.CANCELLED) throw new ResponseStatusException(HttpStatus.CONFLICT,"Reservation already cancelled");
        if(r.getStatus()!=ReservationStatus.CONFIRMED && r.getStatus()!=ReservationStatus.PENDING_PAYMENT)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Only pending-payment or confirmed reservations can be cancelled");
        r.setStatus(ReservationStatus.CANCELLED);
        // Keep ReservationRoom rows for audit/history. Availability query excludes CANCELLED.
        reservations.save(r);
    }

    private void persistCancellationReasonOnPayments(Reservation reservation, String reason) {
        if (reason == null || reason.isBlank()) {
            return;
        }

        List<ReservationPayment> reservationPayments =
                payments.findAllByReservationIdForUpdate(reservation.getReservationId());

        reservationPayments.stream()
                .filter(payment -> payment.getPaymentType() != ReservationPaymentType.REFUND)
                .forEach(payment -> payment.setNote(reason.trim()));

        payments.saveAll(reservationPayments);
    }

    private void verifyGuest(Reservation r,String email,String phone){
        String e=email==null?null:email.trim().toLowerCase(Locale.ROOT);
        String p=phone==null?null:phone.replaceAll("[^0-9+]","");
        boolean matched=guests.findAllByReservationId(r.getReservationId()).stream().map(ReservationGuest::getGuest)
                .filter(Objects::nonNull).anyMatch(g ->
                        (e==null || (g.getEmail()!=null && g.getEmail().trim().equalsIgnoreCase(e))) &&
                        (p==null || (g.getPhone()!=null && g.getPhone().replaceAll("[^0-9+]","").equals(p))));
        if(!matched) throw notFound();
    }
    private ResponseStatusException notFound(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Reservation not found");}
}
