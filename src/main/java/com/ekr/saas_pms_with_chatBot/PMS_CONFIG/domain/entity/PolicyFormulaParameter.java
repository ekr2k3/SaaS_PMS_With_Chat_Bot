package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "policy_formula_parameter")
@IdClass(
        PolicyFormulaParameter.PolicyFormulaParameterId.class
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyFormulaParameter {

    @Id
    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Id
    @Column(name = "policy_formula_id", nullable = false)
    private Long policyFormulaId;

    @Id
    @Column(name = "param_name", nullable = false, length = 100)
    private String paramName;

    @Column(name = "param_value", nullable = false)
    private String paramValue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "property_id",
            insertable = false,
            updatable = false
    )
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "policy_formula_id",
            insertable = false,
            updatable = false
    )
    private PolicyFormula policyFormula;

    public static class PolicyFormulaParameterId
            implements Serializable {

        private Long propertyId;
        private Long policyFormulaId;
        private String paramName;

        public PolicyFormulaParameterId() {
        }

        public PolicyFormulaParameterId(
                Long propertyId,
                Long policyFormulaId,
                String paramName
        ) {
            this.propertyId = propertyId;
            this.policyFormulaId = policyFormulaId;
            this.paramName = paramName;
        }

        public Long getPropertyId() {
            return propertyId;
        }

        public void setPropertyId(Long propertyId) {
            this.propertyId = propertyId;
        }

        public Long getPolicyFormulaId() {
            return policyFormulaId;
        }

        public void setPolicyFormulaId(Long policyFormulaId) {
            this.policyFormulaId = policyFormulaId;
        }

        public String getParamName() {
            return paramName;
        }

        public void setParamName(String paramName) {
            this.paramName = paramName;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }

            if (!(object instanceof PolicyFormulaParameterId that)) {
                return false;
            }

            return Objects.equals(propertyId, that.propertyId)
                    && Objects.equals(
                    policyFormulaId,
                    that.policyFormulaId
            )
                    && Objects.equals(paramName, that.paramName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    propertyId,
                    policyFormulaId,
                    paramName
            );
        }
    }
}