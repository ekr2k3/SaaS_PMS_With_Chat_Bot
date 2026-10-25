package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.hotelpolicy.getformulas;



import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormula;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PolicyFormulaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
public class GetPolicyFormulasService {

    private static final String ACTIVE = "ACTIVE";

    private final PolicyFormulaRepository policyFormulaRepository;

    public GetPolicyFormulasService(
            PolicyFormulaRepository policyFormulaRepository
    ) {
        this.policyFormulaRepository = policyFormulaRepository;
    }

    @Transactional(readOnly = true)
    public List<GetPolicyFormulasResponse> getPolicyFormulas(
            String policyType
    ) {
        return policyFormulaRepository.findAll()
                .stream()
                // Chỉ cung cấp công thức đang hoạt động.
                .filter(formula ->
                        ACTIVE.equalsIgnoreCase(formula.getStatus())
                )
                // Nếu có policyType thì lọc theo nhóm.
                .filter(formula ->
                        !StringUtils.hasText(policyType)
                                || formula.getPolicyType()
                                .equalsIgnoreCase(policyType.trim())
                )
                // Giữ thứ tự ổn định theo ID.
                .sorted(
                        Comparator.comparing(
                                PolicyFormula::getPolicyFormulaId
                        )
                )
                .map(GetPolicyFormulasResponse::from)
                .toList();
    }
}
