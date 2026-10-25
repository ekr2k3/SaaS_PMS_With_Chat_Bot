
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.hotelpolicy.configure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PolicyFormula;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PolicyFormulaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class ConfigureHotelPoliciesValidator {

    private final PolicyFormulaRepository policyFormulaRepository;
    private final ObjectMapper objectMapper;

    public ConfigureHotelPoliciesValidator(
            PolicyFormulaRepository policyFormulaRepository,
            ObjectMapper objectMapper
    ) {
        this.policyFormulaRepository = policyFormulaRepository;
        this.objectMapper = objectMapper;
    }

    public List<ValidatedPolicy> validate(
            ConfigureHotelPoliciesRequest request
    ) {
        if (request == null || request.policies() == null) {
            throw badRequest("Danh sach policies khong duoc null");
        }

        List<ValidatedPolicy> result = new ArrayList<>();
        Set<Long> formulaIds = new HashSet<>();
        Set<String> policyTypes = new HashSet<>();

        for (ConfigureHotelPoliciesRequest.PolicyConfiguration item
                : request.policies()) {

            if (item == null || item.policyFormulaId() == null) {
                throw badRequest("policyFormulaId la bat buoc");
            }

            if (!formulaIds.add(item.policyFormulaId())) {
                throw badRequest("Khong duoc chon trung policyFormulaId");
            }

            PolicyFormula formula = policyFormulaRepository
                    .findById(item.policyFormulaId())
                    .orElseThrow(() -> badRequest(
                            "Khong tim thay Policy Formula: "
                                    + item.policyFormulaId()
                    ));

            if (!"ACTIVE".equalsIgnoreCase(formula.getStatus())) {
                throw badRequest(
                        "Cong thuc khong ACTIVE: "
                                + formula.getName()
                );
            }

            if (!policyTypes.add(formula.getPolicyType())) {
                throw badRequest(
                        "Chi duoc chon mot cong thuc cho Policy type: "
                                + formula.getPolicyType()
                );
            }

            Map<String, String> parameters = normalizeParameters(
                    item.parameters()
            );

            validateParameters(formula, parameters);

            result.add(new ValidatedPolicy(formula, parameters));
        }

        return List.copyOf(result);
    }

    private Map<String, String> normalizeParameters(
            Map<String, String> parameters
    ) {
        if (parameters == null) {
            return Map.of();
        }

        Map<String, String> normalized = new TreeMap<>();

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            if (!StringUtils.hasText(name)
                    || !StringUtils.hasText(value)) {
                throw badRequest(
                        "Ten va gia tri tham so khong duoc rong"
                );
            }

            if (normalized.put(name, value.trim()) != null) {
                throw badRequest("Tham so bi trung: " + name);
            }
        }

        return Collections.unmodifiableMap(normalized);
    }

    private void validateParameters(
            PolicyFormula formula,
            Map<String, String> supplied
    ) {
        JsonNode definition;

        try {
            definition = objectMapper.readTree(
                    formula.getFormulaDefinition()
            );
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Formula definition khong hop le: "
                            + formula.getName(),
                    exception
            );
        }

        JsonNode definitions = definition.path("parameters");

        if (!definitions.isArray()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Formula definition thieu mang parameters: "
                            + formula.getName()
            );
        }

        Set<String> allowedNames = new HashSet<>();

        for (JsonNode parameter : definitions) {
            String name = parameter.path("name").asText(null);
            String type = parameter.path("type").asText(null);

            if (!StringUtils.hasText(name)
                    || !StringUtils.hasText(type)
                    || !allowedNames.add(name)) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Dinh nghia tham so khong hop le: "
                                + formula.getName()
                );
            }

            String value = supplied.get(name);
            boolean required = parameter.path("required")
                    .asBoolean(false);

            if (!StringUtils.hasText(value)) {
                if (required) {
                    throw badRequest(
                            "Thieu tham so bat buoc: " + name
                    );
                }
                continue;
            }

            validateValue(name, value, type, parameter);
        }

        Set<String> unknown = new HashSet<>(supplied.keySet());
        unknown.removeAll(allowedNames);

        if (!unknown.isEmpty()) {
            throw badRequest(
                    "Tham so khong duoc ho tro: " + unknown
            );
        }
    }

    private void validateValue(
            String name,
            String value,
            String type,
            JsonNode definition
    ) {
        try {
            switch (type.toUpperCase(Locale.ROOT)) {
                case "INTEGER" -> {
                    long number = Long.parseLong(value);
                    validateNumericBounds(
                            name,
                            BigDecimal.valueOf(number),
                            definition
                    );
                }
                case "DECIMAL" -> {
                    BigDecimal number = new BigDecimal(value);
                    validateNumericBounds(name, number, definition);
                }
                case "TIME" -> LocalTime.parse(value);
                case "STRING" -> {
                    // Chuoi khong rong da duoc kiem tra o tren.
                }
                default -> throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Kieu tham so chua duoc ho tro: " + type
                );
            }
        } catch (NumberFormatException | DateTimeParseException exception) {
            throw badRequest(
                    "Gia tri khong dung kieu " + type
                            + " cho tham so " + name
            );
        }
    }

    private void validateNumericBounds(
            String name,
            BigDecimal value,
            JsonNode definition
    ) {
        if (definition.has("min")
                && value.compareTo(
                definition.get("min").decimalValue()
        ) < 0) {
            throw badRequest(
                    "Gia tri " + name + " nho hon min cho phep"
            );
        }

        if (definition.has("max")
                && value.compareTo(
                definition.get("max").decimalValue()
        ) > 0) {
            throw badRequest(
                    "Gia tri " + name + " vuot qua max cho phep"
            );
        }

        if (definition.has("minExclusive")
                && value.compareTo(
                definition.get("minExclusive").decimalValue()
        ) <= 0) {
            throw badRequest(
                    "Gia tri " + name + " phai lon hon minExclusive"
            );
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    public record ValidatedPolicy(
            PolicyFormula formula,
            Map<String, String> parameters
    ) {
    }
}
