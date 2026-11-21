package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refundquote;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.AppliedPms;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormula;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormulaParameter;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.AppliedPmsRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PolicyFormulaParameterRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PolicyFormulaRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.*;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.*;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.RefundAccessService;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.get.GetReservationValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class GetRefundQuoteService {
    private final ReservationRepository reservations;
    private final ReservationGuestRepository guests;
    private final ReservationPaymentRepository payments;
    private final AppliedPmsRepository appliedPolicies;
    private final PolicyFormulaRepository formulas;
    private final PolicyFormulaParameterRepository parameters;
    private final RefundAccessService access;
    private final GetReservationValidator validator;
    private final ObjectMapper mapper;

    public GetRefundQuoteService(ReservationRepository reservations,
            ReservationGuestRepository guests, ReservationPaymentRepository payments,
            AppliedPmsRepository appliedPolicies, PolicyFormulaRepository formulas,
            PolicyFormulaParameterRepository parameters, RefundAccessService access,
            GetReservationValidator validator, ObjectMapper mapper) {
        this.reservations=reservations; this.guests=guests; this.payments=payments;
        this.appliedPolicies=appliedPolicies; this.formulas=formulas; this.parameters=parameters;
        this.access=access; this.validator=validator; this.mapper=mapper;
    }

    @Transactional(readOnly = true)
    public GetRefundQuoteResponse getGuestQuote(String code, String email, String phone) {
        validator.validate(code,email,phone);
        Reservation r = findReservation(code);
        verifyGuest(r,email,phone);
        return calculate(r);
    }

    @Transactional(readOnly = true)
    public GetRefundQuoteResponse getReceptionistQuote(Long propertyId,String code,String email,String phone) {
        access.requirePermission(propertyId,"REFUND_CALCULATE");
        validator.validate(code,email,phone);
        Reservation r=findReservation(code);
        if(r.getProperty()==null || !propertyId.equals(r.getProperty().getPropertyId())) throw notFound();
        verifyGuest(r,email,phone);
        return calculate(r);
    }

    private Reservation findReservation(String code) {
        return reservations.findByReservationCode(code.trim()).orElseThrow(this::notFound);
    }

    private void verifyGuest(Reservation r,String email,String phone) {
        String e=email==null?null:email.trim().toLowerCase(Locale.ROOT);
        String p=phone==null?null:phone.replaceAll("[^0-9+]","");
        boolean matched=guests.findAllByReservationId(r.getReservationId()).stream().map(ReservationGuest::getGuest)
            .filter(Objects::nonNull).anyMatch(g ->
                (e==null || (g.getEmail()!=null && g.getEmail().trim().equalsIgnoreCase(e))) &&
                (p==null || (g.getPhone()!=null && g.getPhone().replaceAll("[^0-9+]","").equals(p))));
        if(!matched) throw notFound();
    }

    @Transactional(readOnly = true)
    public GetRefundQuoteResponse getInternalQuote(Reservation reservation) {
        return calculate(reservation);
    }

    private GetRefundQuoteResponse calculate(Reservation r) {
        Long propertyId=r.getProperty().getPropertyId();
        List<AppliedPms> applied=appliedPolicies.findAllByPropertyIdOrderByPolicyFormulaIdAsc(propertyId);
        PolicyFormula formula=applied.stream().map(AppliedPms::getPolicyFormula).filter(Objects::nonNull)
            .filter(f -> "CANCELLATION".equalsIgnoreCase(f.getPolicyType()) && "ACTIVE".equalsIgnoreCase(f.getStatus()))
            .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,"Property has no active cancellation policy"));
        JsonNode definition;
        try { definition=mapper.readTree(formula.getFormulaDefinition()); }
        catch(Exception ex){ throw new IllegalStateException("Invalid cancellation formula definition",ex); }
        String kind=definition.path("kind").asText();
        Integer freeDays=null;
        if("FREE_CANCELLATION_BY_DAYS".equals(kind)) {
            freeDays=parameters.findAllByPropertyIdAndPolicyFormulaId(propertyId,formula.getPolicyFormulaId()).stream()
                .filter(p -> "freeCancellationDays".equals(p.getParamName())).map(PolicyFormulaParameter::getParamValue)
                .map(v -> { try{return Integer.valueOf(v);}catch(Exception ex){return null;} }).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,"Cancellation policy parameter is not configured"));
        } else if(!"NON_REFUNDABLE".equals(kind)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Unsupported cancellation formula: "+kind);
        }
        long days=Math.max(0,ChronoUnit.DAYS.between(LocalDate.now(),r.getCheckInDate()));
        BigDecimal paid=payments.findAllByReservation_ReservationIdOrderByReservationPaymentIdDesc(r.getReservationId()).stream()
            .filter(p -> (p.getPaymentType()==ReservationPaymentType.DEPOSIT || p.getPaymentType()==ReservationPaymentType.BALANCE) && (p.getStatus()==ReservationPaymentStatus.SUCCEEDED || p.getStatus()==ReservationPaymentStatus.REFUNDED))
            .map(ReservationPayment::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
        List<ReservationPayment> refundPayments=payments.findAllByReservation_ReservationIdOrderByReservationPaymentIdDesc(r.getReservationId()).stream()
            .filter(p -> p.getPaymentType()==ReservationPaymentType.REFUND).toList();
        BigDecimal refunded=refundPayments.stream()
            .filter(p -> p.getStatus()==ReservationPaymentStatus.SUCCEEDED)
            .map(ReservationPayment::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal pendingRefund=refundPayments.stream()
            .filter(p -> p.getStatus()==ReservationPaymentStatus.PENDING || p.getStatus()==ReservationPaymentStatus.PROCESSING)
            .map(ReservationPayment::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
        boolean policyAllows="FREE_CANCELLATION_BY_DAYS".equals(kind) && days>=freeDays;
        BigDecimal remaining=paid.subtract(refunded).subtract(pendingRefund).max(BigDecimal.ZERO);
        BigDecimal refundable=policyAllows?remaining:BigDecimal.ZERO;
        return new GetRefundQuoteResponse(r.getReservationCode(),r.getStatus().name(),r.getCheckInDate(),kind,freeDays,days,paid,refunded,pendingRefund,refundable,r.getCurrency(),refundable.signum()>0);
    }
    private ResponseStatusException notFound(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Reservation not found");}
}
