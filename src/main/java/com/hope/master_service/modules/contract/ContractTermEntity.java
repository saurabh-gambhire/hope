package com.hope.master_service.modules.contract;

import com.hope.master_service.dto.contract.ContractTerm;
import com.hope.master_service.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "contract_term")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "contract")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ContractTermEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private ContractEntity contract;

    @Column(nullable = false)
    private String purchaseOrderNumber;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
    }

    public ContractTerm toDto() {
        return ContractTerm.builder()
                .uuid(this.uuid)
                .purchaseOrderNumber(this.purchaseOrderNumber)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .build();
    }

    public static ContractTermEntity fromDto(ContractTerm dto, ContractEntity contract) {
        return ContractTermEntity.builder()
                .contract(contract)
                .purchaseOrderNumber(dto.getPurchaseOrderNumber())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();
    }
}
