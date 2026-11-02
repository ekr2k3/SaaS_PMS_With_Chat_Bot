
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "service_catalog",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_service_catalog_service_type",
                        columnNames = "service_type"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(
            name = "service_type",
            nullable = false,
            length = 50,
            unique = true
    )
    private String serviceType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    private String status;
}