package com.hope.master_service.modules.contract;

import com.hope.master_service.dto.contract.ContractOffering;
import com.hope.master_service.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "contract_offering")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "contract")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ContractOfferingEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private ContractEntity contract;

    @Column(nullable = false)
    private String offeringName;

    private String serviceCode;

    @Column(precision = 12, scale = 2)
    private BigDecimal defaultRate;

    @Column(precision = 12, scale = 2)
    private BigDecimal templateRate;

    private boolean active;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
        this.active = true;
    }

    public ContractOffering toDto() {
        return ContractOffering.builder()
                .uuid(this.uuid)
                .offeringName(this.offeringName)
                .serviceCode(this.serviceCode)
                .defaultRate(this.defaultRate)
                .templateRate(this.templateRate)
                .active(this.active)
                .build();
    }

    public static ContractOfferingEntity fromDto(ContractOffering dto, ContractEntity contract) {
        return ContractOfferingEntity.builder()
                .contract(contract)
                .offeringName(dto.getOfferingName())
                .serviceCode(dto.getServiceCode())
                .defaultRate(dto.getDefaultRate())
                .templateRate(dto.getTemplateRate())
                .build();
    }
}
