
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "fnb_menu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FnbMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fnb_menu_id")
    private Long fnbMenuId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "max_quantity", nullable = false)
    private Integer maxQuantity;

    @Column(name = "actual_quantity", nullable = false)
    private Integer actualQuantity;

    @Column(
            name = "purchase_price",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal purchasePrice;

    @Column(
            name = "selling_price_percent",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal sellingPricePercent;

    @Column(
            name = "selling_price",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal sellingPrice;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;
}