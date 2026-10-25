package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.seeder;



import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormula;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PolicyFormulaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class PolicyFormulaSeeder implements ApplicationRunner {

    private static final String ACTIVE = "ACTIVE";

    private final PolicyFormulaRepository policyFormulaRepository;

    public PolicyFormulaSeeder(
            PolicyFormulaRepository policyFormulaRepository
    ) {
        this.policyFormulaRepository = policyFormulaRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (PolicyFormula formula : defaultFormulas()) {
            boolean exists =
                    policyFormulaRepository.existsByPolicyTypeAndName(
                            formula.getPolicyType(),
                            formula.getName()
                    );

            if (!exists) {
                policyFormulaRepository.save(formula);
            }
        }
    }

    private List<PolicyFormula> defaultFormulas() {
        return List.of(
                formula(
                        "CANCELLATION",
                        "Hủy miễn phí theo số ngày",
                        """
                        {"kind":"FREE_CANCELLATION_BY_DAYS","parameters":[{"name":"freeCancellationDays","type":"INTEGER","required":true,"min":0}]}
                        """
                ),
                formula(
                        "CANCELLATION",
                        "Không hoàn tiền",
                        """
                        {"kind":"NON_REFUNDABLE","parameters":[]}
                        """
                ),
                formula(
                        "NO_SHOW",
                        "Giữ tiền đặt cọc",
                        """
                        {"kind":"RETAIN_DEPOSIT","parameters":[]}
                        """
                ),
                formula(
                        "NO_SHOW",
                        "Hoàn tiền một phần",
                        """
                        {"kind":"PARTIAL_REFUND","parameters":[{"name":"refundPercentage","type":"DECIMAL","required":true,"min":0,"max":100}]}
                        """
                ),
                formula(
                        "DEPOSIT",
                        "Đặt cọc theo phần trăm",
                        """
                        {"kind":"PERCENTAGE_DEPOSIT","parameters":[{"name":"depositPercentage","type":"DECIMAL","required":true,"minExclusive":0,"max":100}]}
                        """
                ),
                formula(
                        "DEPOSIT",
                        "Đặt cọc số tiền cố định",
                        """
                        {"kind":"FIXED_AMOUNT_DEPOSIT","parameters":[{"name":"depositAmount","type":"DECIMAL","required":true,"minExclusive":0}]}
                        """
                ),
                formula(
                        "CHECK_IN_TIME",
                        "Giờ nhận phòng tiêu chuẩn",
                        """
                        {"kind":"STANDARD_CHECK_IN_TIME","parameters":[{"name":"checkInTime","type":"TIME","required":true}]}
                        """
                ),
                formula(
                        "CHECK_OUT_TIME",
                        "Giờ trả phòng tiêu chuẩn",
                        """
                        {"kind":"STANDARD_CHECK_OUT_TIME","parameters":[{"name":"checkOutTime","type":"TIME","required":true}]}
                        """
                )
        );
    }

    private PolicyFormula formula(
            String policyType,
            String name,
            String definition
    ) {
        return PolicyFormula.builder()
                .policyType(policyType)
                .name(name)
                .formulaDefinition(definition)
                .status(ACTIVE)
                .build();
    }
}
