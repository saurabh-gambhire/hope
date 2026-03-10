package com.hope.master_service.modules.contract;

import com.hope.master_service.dto.contract.ContractSubOrg;
import com.hope.master_service.entity.AuditableEntity;
import com.hope.master_service.modules.organization.SubOrganizationEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "contract_sub_organization", uniqueConstraints = @UniqueConstraint(
        name = "uk_contract_sub_organization",
        columnNames = {"contract_id", "sub_organization_id"}
))
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"contract", "subOrganization"})
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ContractSubOrganizationEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private ContractEntity contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_organization_id", nullable = false)
    private SubOrganizationEntity subOrganization;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
    }

    public ContractSubOrg toDto() {
        return ContractSubOrg.builder()
                .uuid(this.uuid)
                .subOrganizationUuid(this.subOrganization.getUuid())
                .subOrganizationName(this.subOrganization.getName())
                .build();
    }
}
