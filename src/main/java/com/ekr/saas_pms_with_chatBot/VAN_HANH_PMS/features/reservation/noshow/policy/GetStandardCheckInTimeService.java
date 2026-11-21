package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.reservation.noshow.policy;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.AppliedPms;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormulaParameter;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.AppliedPmsRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PolicyFormulaParameterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetStandardCheckInTimeService {

    private static final String POLICY_TYPE = "CHECK_IN_TIME";
    private static final String PARAMETER_NAME = "checkInTime";

    private final AppliedPmsRepository appliedPmsRepository;

    private final PolicyFormulaParameterRepository parameterRepository;

    @Transactional(readOnly = true)
    public LocalTime getCheckInTime(Long propertyId) {

        List<AppliedPms> appliedPolicies =
                appliedPmsRepository
                        .findAllByPropertyIdOrderByPolicyFormulaIdAsc(
                                propertyId
                        );

        AppliedPms checkInPolicy = appliedPolicies.stream()
                .filter(applied -> applied.getPolicyFormula() != null)
                .filter(applied ->
                        POLICY_TYPE.equalsIgnoreCase(
                                applied.getPolicyFormula().getPolicyType()
                        )
                )
                .filter(applied ->
                        "ACTIVE".equalsIgnoreCase(
                                applied.getPolicyFormula().getStatus()
                        )
                )
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Property has not configured an active CHECK_IN_TIME policy"
                ));

        List<PolicyFormulaParameter> parameters =
                parameterRepository
                        .findAllByPropertyIdAndPolicyFormulaId(
                                propertyId,
                                checkInPolicy.getPolicyFormulaId()
                        );

        String configuredTime = parameters.stream()
                .filter(parameter ->
                        PARAMETER_NAME.equalsIgnoreCase(
                                parameter.getParamName()
                        )
                )
                .map(PolicyFormulaParameter::getParamValue)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "CHECK_IN_TIME policy is missing checkInTime parameter"
                ));

        try {
            return LocalTime.parse(configuredTime.trim());
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Configured checkInTime is invalid. Expected HH:mm or HH:mm:ss"
            );
        }
    }
}
