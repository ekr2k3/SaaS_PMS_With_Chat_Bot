package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "policy_formula")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyFormula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_formula_id")
    private Long policyFormulaId;

    @Column(name = "policy_type", nullable = false, length = 50)
    private String policyType;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(
            name = "formula_definition",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String formulaDefinition;

    @Column(name = "status", nullable = false, length = 30)
    private String status;
}
