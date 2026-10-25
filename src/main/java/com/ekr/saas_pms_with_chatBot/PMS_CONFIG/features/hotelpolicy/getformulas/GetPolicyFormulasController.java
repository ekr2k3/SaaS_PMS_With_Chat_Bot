
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.hotelpolicy.getformulas;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/policy-formulas")
public class GetPolicyFormulasController {

    private final GetPolicyFormulasService getPolicyFormulasService;

    public GetPolicyFormulasController(
            GetPolicyFormulasService getPolicyFormulasService
    ) {
        this.getPolicyFormulasService = getPolicyFormulasService;
    }

    @GetMapping
    public List<GetPolicyFormulasResponse> getPolicyFormulas(
            @RequestParam(required = false) String policyType
    ) {
        return getPolicyFormulasService.getPolicyFormulas(policyType);
    }
}