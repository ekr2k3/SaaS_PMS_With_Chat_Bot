package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.hotelpolicy.get;



import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.AppliedPms;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormula;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormulaParameter;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.AppliedPmsRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PolicyFormulaParameterRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GetHotelPoliciesService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final AppliedPmsRepository appliedPmsRepository;
    private final PolicyFormulaParameterRepository parameterRepository;

    public GetHotelPoliciesService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            AppliedPmsRepository appliedPmsRepository,
            PolicyFormulaParameterRepository parameterRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.appliedPmsRepository = appliedPmsRepository;
        this.parameterRepository = parameterRepository;
    }

    @Transactional(readOnly = true)
    public GetHotelPoliciesResponse getHotelPolicies(
            String ownerEmail,
            Long propertyId
    ) {
        Tenant tenant = getOwnerTenant(ownerEmail);

        Property property = propertyRepository
                .findByPropertyIdAndTenant_TenantId(
                        propertyId,
                        tenant.getTenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay Property"
                ));

        List<AppliedPms> appliedPolicies =
                appliedPmsRepository
                        .findAllByPropertyIdOrderByPolicyFormulaIdAsc(
                                property.getPropertyId()
                        );

        if (appliedPolicies.isEmpty()) {
            return new GetHotelPoliciesResponse(
                    property.getPropertyId(),
                    List.of()
            );
        }

        List<PolicyFormulaParameter> parameters =
                parameterRepository
                        .findAllByPropertyIdOrderByPolicyFormulaIdAscParamNameAsc(
                                property.getPropertyId()
                        );

        Map<Long, List<PolicyFormulaParameter>> parametersByFormulaId =
                parameters.stream()
                        .collect(Collectors.groupingBy(
                                PolicyFormulaParameter::getPolicyFormulaId
                        ));

        List<GetHotelPoliciesResponse.PolicyResponse> policies =
                appliedPolicies.stream()
                        .map(applied -> {
                            PolicyFormula formula =
                                    applied.getPolicyFormula();

                            List<PolicyFormulaParameter> formulaParameters =
                                    parametersByFormulaId.getOrDefault(
                                            applied.getPolicyFormulaId(),
                                            List.of()
                                    );

                            return GetHotelPoliciesResponse.PolicyResponse.from(
                                    formula,
                                    formulaParameters
                            );
                        })
                        .toList();

        return new GetHotelPoliciesResponse(
                property.getPropertyId(),
                policies
        );
    }

    private Tenant getOwnerTenant(String ownerEmail) {
        Account account = accountRepository.findByEmail(ownerEmail);

        if (account == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Khong tim thay Owner"
            );
        }

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tai khoan Owner khong hoat dong"
            );
        }

        return tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Owner chua co Tenant"
                ));
    }
}