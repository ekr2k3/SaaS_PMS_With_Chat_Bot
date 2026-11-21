package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.refund;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.AppliedPms;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormula;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormulaParameter;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.AppliedPmsRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PolicyFormulaParameterRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.Reservation;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.ReservationPayment;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentStatus;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.enums.ReservationPaymentType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class NoShowRefundPolicyService {

    private final AppliedPmsRepository appliedPolicies;

    private final PolicyFormulaParameterRepository parameters;

    private final ObjectMapper mapper;

    public NoShowRefundPolicyService(
            AppliedPmsRepository appliedPolicies,
            PolicyFormulaParameterRepository parameters,
            ObjectMapper mapper
    ) {
        this.appliedPolicies = appliedPolicies;
        this.parameters = parameters;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<NoShowRefundAllocation> calculate(
            Reservation reservation,
            List<ReservationPayment> paymentRows
    ) {

        Long propertyId =
                reservation.getProperty().getPropertyId();

        PolicyFormula formula = findNoShowFormula(propertyId);

        String kind = readFormulaKind(formula);

        int scale = getCurrencyScale(reservation.getCurrency());

        List<ReservationPayment> originals = paymentRows.stream()
                .filter(this::isCapturedOriginalPayment)
                .filter(p -> p.getAmount() != null)
                .filter(p -> p.getAmount().signum() > 0)
                .toList();

        if ("RETAIN_DEPOSIT".equals(kind)) {
            return calculateRetainDeposit(
                    originals,
                    paymentRows,
                    scale
            );
        }

        if ("PARTIAL_REFUND".equals(kind)) {

            BigDecimal percentage =
                    getRefundPercentage(propertyId, formula);

            return calculatePartialRefund(
                    originals,
                    paymentRows,
                    percentage,
                    scale
            );
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Unsupported NO_SHOW formula: " + kind
        );
    }

    private PolicyFormula findNoShowFormula(Long propertyId) {

        List<PolicyFormula> formulas =
                appliedPolicies
                        .findAllByPropertyIdOrderByPolicyFormulaIdAsc(propertyId)
                        .stream()
                        .map(AppliedPms::getPolicyFormula)
                        .filter(Objects::nonNull)
                        .filter(f ->
                                "NO_SHOW".equalsIgnoreCase(f.getPolicyType())
                        )
                        .filter(f ->
                                "ACTIVE".equalsIgnoreCase(f.getStatus())
                        )
                        .toList();

        if (formulas.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Property has no active NO_SHOW policy"
            );
        }

        if (formulas.size() > 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Property has multiple active NO_SHOW policies"
            );
        }

        return formulas.get(0);
    }

    private String readFormulaKind(PolicyFormula formula) {

        try {
            JsonNode definition =
                    mapper.readTree(formula.getFormulaDefinition());

            String kind = definition.path("kind").asText();

            if (kind == null || kind.isBlank()) {
                throw new IllegalStateException(
                        "Missing NO_SHOW formula kind"
                );
            }

            return kind;

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Invalid NO_SHOW formula definition",
                    ex
            );
        }
    }

    private BigDecimal getRefundPercentage(
            Long propertyId,
            PolicyFormula formula
    ) {

        String value = parameters
                .findAllByPropertyIdAndPolicyFormulaId(
                        propertyId,
                        formula.getPolicyFormulaId()
                )
                .stream()
                .filter(p ->
                        "refundPercentage".equals(p.getParamName())
                )
                .map(PolicyFormulaParameter::getParamValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "NO_SHOW refundPercentage is not configured"
                ));

        BigDecimal percentage;

        try {
            percentage = new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid NO_SHOW refundPercentage"
            );
        }

        if (percentage.compareTo(BigDecimal.ZERO) < 0
                || percentage.compareTo(new BigDecimal("100")) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "NO_SHOW refundPercentage must be between 0 and 100"
            );
        }

        return percentage;
    }

    private List<NoShowRefundAllocation> calculateRetainDeposit(
            List<ReservationPayment> originals,
            List<ReservationPayment> allPayments,
            int scale
    ) {

        List<NoShowRefundAllocation> allocations =
                new ArrayList<>();

        for (ReservationPayment original : originals) {

            if (original.getPaymentType()
                    != ReservationPaymentType.BALANCE) {
                continue;
            }

            BigDecimal remaining = getRemainingRefundableAmount(
                    original,
                    allPayments,
                    scale
            );

            if (remaining.signum() > 0) {
                allocations.add(
                        new NoShowRefundAllocation(
                                original,
                                remaining
                        )
                );
            }
        }

        return allocations;
    }

    private List<NoShowRefundAllocation> calculatePartialRefund(
            List<ReservationPayment> originals,
            List<ReservationPayment> allPayments,
            BigDecimal percentage,
            int scale
    ) {

        BigDecimal totalCaptured = originals.stream()
                .map(ReservationPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal refundTarget = totalCaptured
                .multiply(percentage)
                .divide(
                        new BigDecimal("100"),
                        scale,
                        RoundingMode.HALF_UP
                );

        BigDecimal existingRefunds = allPayments.stream()
                .filter(this::isActiveRefund)
                .map(ReservationPayment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingTarget = refundTarget
                .subtract(existingRefunds)
                .max(BigDecimal.ZERO);

        List<NoShowRefundAllocation> allocations =
                new ArrayList<>();

        // Ưu tiên hoàn BALANCE trước DEPOSIT.
        List<ReservationPayment> ordered = originals.stream()
                .sorted(
                        Comparator.<ReservationPayment>comparingInt(
                                p -> p.getPaymentType()
                                        == ReservationPaymentType.BALANCE
                                        ? 0 : 1
                        ).thenComparing(
                                ReservationPayment::getReservationPaymentId
                        )
                )
                .toList();

        for (ReservationPayment original : ordered) {

            if (remainingTarget.signum() <= 0) {
                break;
            }

            BigDecimal capacity = getRemainingRefundableAmount(
                    original,
                    allPayments,
                    scale
            );

            BigDecimal allocation =
                    capacity.min(remainingTarget);

            if (allocation.signum() > 0) {

                allocations.add(
                        new NoShowRefundAllocation(
                                original,
                                allocation
                        )
                );

                remainingTarget =
                        remainingTarget.subtract(allocation);
            }
        }

        return allocations;
    }

    private BigDecimal getRemainingRefundableAmount(
            ReservationPayment original,
            List<ReservationPayment> allPayments,
            int scale
    ) {

        String transactionId = getOriginalTransactionId(original);

        if (transactionId == null || transactionId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Original Payment has no Stripe transaction identifier"
            );
        }

        BigDecimal alreadyRefunded = allPayments.stream()
                .filter(this::isActiveRefund)
                .filter(refund ->
                        transactionId.equals(refund.getTransactionId())
                )
                .map(ReservationPayment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return original.getAmount()
                .subtract(alreadyRefunded)
                .max(BigDecimal.ZERO)
                .setScale(scale, RoundingMode.HALF_UP);
    }

    private String getOriginalTransactionId(
            ReservationPayment original
    ) {

        if (original.getPaymentIntentId() != null
                && !original.getPaymentIntentId().isBlank()) {
            return original.getPaymentIntentId();
        }

        return original.getTransactionId();
    }

    private boolean isCapturedOriginalPayment(
            ReservationPayment payment
    ) {

        return (payment.getPaymentType()
                == ReservationPaymentType.DEPOSIT
                || payment.getPaymentType()
                == ReservationPaymentType.BALANCE)
                && (payment.getStatus()
                == ReservationPaymentStatus.SUCCEEDED
                || payment.getStatus()
                == ReservationPaymentStatus.REFUNDED);
    }

    private boolean isActiveRefund(
            ReservationPayment payment
    ) {

        return payment.getPaymentType()
                == ReservationPaymentType.REFUND
                && (payment.getStatus()
                == ReservationPaymentStatus.SUCCEEDED
                || payment.getStatus()
                == ReservationPaymentStatus.PROCESSING
                || payment.getStatus()
                == ReservationPaymentStatus.PENDING);
    }

    private int getCurrencyScale(String currency) {

        if (currency == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reservation currency is missing"
            );
        }

        return switch (currency.toUpperCase()) {
            case "VND", "JPY", "KRW" -> 0;
            default -> 2;
        };
    }
}
