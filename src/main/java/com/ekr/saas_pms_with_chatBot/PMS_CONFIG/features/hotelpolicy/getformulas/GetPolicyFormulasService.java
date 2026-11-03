
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.hotelpolicy.getformulas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormula;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PolicyFormulaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class GetPolicyFormulasService {

    private static final String ACTIVE = "ACTIVE";

    private final PolicyFormulaRepository policyFormulaRepository;
    private final ObjectMapper objectMapper;

    public GetPolicyFormulasService(
            PolicyFormulaRepository policyFormulaRepository,
            ObjectMapper objectMapper
    ) {
        this.policyFormulaRepository = policyFormulaRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<GetPolicyFormulasResponse> getPolicyFormulas(
            String policyType
    ) {
        return policyFormulaRepository.findAll()
                .stream()
                .filter(formula ->
                        ACTIVE.equalsIgnoreCase(formula.getStatus())
                )
                .filter(formula ->
                        !StringUtils.hasText(policyType)
                                || formula.getPolicyType()
                                .equalsIgnoreCase(policyType.trim())
                )
                .sorted(
                        Comparator.comparing(
                                PolicyFormula::getPolicyFormulaId
                        )
                )
                .map(this::toResponse)
                .toList();
    }

    private GetPolicyFormulasResponse toResponse(
            PolicyFormula formula
    ) {
        return GetPolicyFormulasResponse.from(
                formula,
                readParameterDefinitions(formula)
        );
    }

    private List<GetPolicyFormulasResponse.ParameterDefinition>
    readParameterDefinitions(PolicyFormula formula) {

        JsonNode formulaDefinition;

        try {
            formulaDefinition = objectMapper.readTree(
                    formula.getFormulaDefinition()
            );
        } catch (Exception exception) {
            throw invalidFormulaDefinition(formula, exception);
        }

        JsonNode parametersNode = formulaDefinition == null
                ? null
                : formulaDefinition.get("parameters");

        if (parametersNode == null || !parametersNode.isArray()) {
            throw invalidFormulaDefinition(formula, null);
        }

        List<GetPolicyFormulasResponse.ParameterDefinition> parameters =
                new ArrayList<>();

        Set<String> parameterNames = new HashSet<>();

        for (JsonNode parameterNode : parametersNode) {

            String name = parameterNode.path("name").asText(null);
            String type = parameterNode.path("type").asText(null);

            if (!StringUtils.hasText(name)
                    || !StringUtils.hasText(type)
                    || !parameterNames.add(name)) {
                throw invalidFormulaDefinition(formula, null);
            }

            parameters.add(
                    new GetPolicyFormulasResponse.ParameterDefinition(
                            name,
                            type.toUpperCase(Locale.ROOT),
                            parameterNode.path("required").asBoolean(false),
                            readOptionalDecimal(parameterNode, "min", formula),
                            readOptionalDecimal(parameterNode, "max", formula),
                            readOptionalDecimal(
                                    parameterNode,
                                    "minExclusive",
                                    formula
                            )
                    )
            );
        }

        return List.copyOf(parameters);
    }

    private BigDecimal readOptionalDecimal(
            JsonNode parameterNode,
            String fieldName,
            PolicyFormula formula
    ) {
        JsonNode valueNode = parameterNode.get(fieldName);

        if (valueNode == null || valueNode.isNull()) {
            return null;
        }

        if (!valueNode.isNumber()) {
            throw invalidFormulaDefinition(formula, null);
        }

        return valueNode.decimalValue();
    }

    private ResponseStatusException invalidFormulaDefinition(
            PolicyFormula formula,
            Exception cause
    ) {
        String message = "Formula definition khong hop le: "
                + formula.getName();

        return cause == null
                ? new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                message
        )
                : new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                message,
                cause
        );
    }
}