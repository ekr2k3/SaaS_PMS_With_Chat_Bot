package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity;


import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "applied_pms")
@IdClass(AppliedPms.AppliedPmsId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppliedPms {

    @Id
    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Id
    @Column(name = "policy_formula_id", nullable = false)
    private Long policyFormulaId;

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

    public static class AppliedPmsId implements Serializable {

        private Long propertyId;
        private Long policyFormulaId;

        public AppliedPmsId() {
        }

        public AppliedPmsId(
                Long propertyId,
                Long policyFormulaId
        ) {
            this.propertyId = propertyId;
            this.policyFormulaId = policyFormulaId;
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

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }

            if (!(object instanceof AppliedPmsId that)) {
                return false;
            }

            return Objects.equals(propertyId, that.propertyId)
                    && Objects.equals(
                    policyFormulaId,
                    that.policyFormulaId
            );
        }

        @Override
        public int hashCode() {
            return Objects.hash(propertyId, policyFormulaId);
        }
    }
}
