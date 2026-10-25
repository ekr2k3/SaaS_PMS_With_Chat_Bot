
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.hotelpolicy.configure;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.AppliedPms;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormula;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormulaParameter;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Property;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.AppliedPmsRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PolicyFormulaParameterRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PropertyRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.hotelpolicy.configure.ConfigureHotelPoliciesValidator.ValidatedPolicy;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConfigureHotelPoliciesService {

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final AppliedPmsRepository appliedPmsRepository;
    private final PolicyFormulaParameterRepository parameterRepository;
    private final ConfigureHotelPoliciesValidator validator;

    public ConfigureHotelPoliciesService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            AppliedPmsRepository appliedPmsRepository,
            PolicyFormulaParameterRepository parameterRepository,
            ConfigureHotelPoliciesValidator validator
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.appliedPmsRepository = appliedPmsRepository;
        this.parameterRepository = parameterRepository;
        this.validator = validator;
    }

    @Transactional
    public ConfigureHotelPoliciesResponse configure(
            String ownerEmail,
            Long propertyId,
            ConfigureHotelPoliciesRequest request
    ) {
        // 1. Validate toan bo request truoc khi ghi du lieu.
        List<ValidatedPolicy> desired = validator.validate(request);

        // 2. Xac minh Property thuoc Tenant cua Owner.
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

        // 3. Doc trang thai hien tai.
        List<AppliedPms> current =
                appliedPmsRepository
                        .findAllByPropertyIdOrderByPolicyFormulaIdAsc(
                                property.getPropertyId()
                        );

        List<PolicyFormulaParameter> currentParameters =
                parameterRepository
                        .findAllByPropertyIdOrderByPolicyFormulaIdAscParamNameAsc(
                                property.getPropertyId()
                        );

        Map<Long, List<PolicyFormulaParameter>> parametersByFormula =
                currentParameters.stream()
                        .collect(Collectors.groupingBy(
                                PolicyFormulaParameter::getPolicyFormulaId
                        ));

        Map<String, AppliedPms> currentByType = new HashMap<>();

        for (AppliedPms applied : current) {
            String type = applied.getPolicyFormula().getPolicyType();

            if (currentByType.put(type, applied) != null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Property dang co nhieu cong thuc cho Policy type "
                                + type
                );
            }
        }

        Map<String, ValidatedPolicy> desiredByType =
                desired.stream().collect(Collectors.toMap(
                        item -> item.formula().getPolicyType(),
                        Function.identity()
                ));

        // 4. Xoa cac Policy bi bo chon va thay cong thuc neu can.
        for (AppliedPms existing : current) {
            PolicyFormula oldFormula = existing.getPolicyFormula();
            String type = oldFormula.getPolicyType();
            ValidatedPolicy requested = desiredByType.get(type);

            if (requested == null
                    || !Objects.equals(
                    requested.formula().getPolicyFormulaId(),
                    oldFormula.getPolicyFormulaId()
            )) {

                deleteParameters(
                        property.getPropertyId(),
                        oldFormula.getPolicyFormulaId()
                );

                appliedPmsRepository.delete(existing);
            }
        }

        // 5. Them cong thuc moi hoac cap nhat tham so.
        for (ValidatedPolicy requested : desired) {
            PolicyFormula formula = requested.formula();
            Long formulaId = formula.getPolicyFormulaId();

            AppliedPms previous = currentByType.get(
                    formula.getPolicyType()
            );

            boolean sameFormula = previous != null
                    && Objects.equals(
                    previous.getPolicyFormulaId(),
                    formulaId
            );

            if (!sameFormula) {
                appliedPmsRepository.save(
                        new AppliedPms(
                                property.getPropertyId(),
                                formulaId,
                                null,
                                null
                        )
                );

                saveParameters(
                        property.getPropertyId(),
                        formulaId,
                        requested.parameters()
                );

                continue;
            }

            Map<String, String> oldValues =
                    parametersByFormula.getOrDefault(
                            formulaId,
                            List.of()
                    ).stream().collect(Collectors.toMap(
                            PolicyFormulaParameter::getParamName,
                            PolicyFormulaParameter::getParamValue
                    ));

            if (!oldValues.equals(requested.parameters())) {
                deleteParameters(property.getPropertyId(), formulaId);

                saveParameters(
                        property.getPropertyId(),
                        formulaId,
                        requested.parameters()
                );
            }
        }

        // 6. Tra ve trang thai mong muon da duoc luu.
        List<ConfigureHotelPoliciesResponse.ConfiguredPolicy> response =
                desired.stream()
                        .map(item -> new ConfigureHotelPoliciesResponse
                                .ConfiguredPolicy(
                                item.formula().getPolicyFormulaId(),
                                item.formula().getPolicyType(),
                                item.formula().getName(),
                                item.formula().getStatus()
                        ))
                        .toList();

        return new ConfigureHotelPoliciesResponse(
                property.getPropertyId(),
                "Hotel policies configured successfully",
                response
        );
    }

    private void deleteParameters(
            Long propertyId,
            Long policyFormulaId
    ) {
        parameterRepository
                .deleteAllByPropertyIdAndPolicyFormulaId(
                        propertyId,
                        policyFormulaId
                );
    }

    private void saveParameters(
            Long propertyId,
            Long policyFormulaId,
            Map<String, String> parameters
    ) {
        for (Map.Entry<String, String> parameter
                : parameters.entrySet()) {

            parameterRepository.save(
                    new PolicyFormulaParameter(
                            propertyId,
                            policyFormulaId,
                            parameter.getKey(),
                            parameter.getValue(),
                            null,
                            null
                    )
            );
        }
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

        Tenant tenant = tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Owner chua co Tenant"
                ));

        if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tenant khong hoat dong"
            );
        }

        return tenant;
    }
}
