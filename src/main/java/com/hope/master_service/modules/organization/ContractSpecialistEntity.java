package com.hope.master_service.modules.organization;

import com.hope.master_service.dto.enums.PhoneType;
import com.hope.master_service.dto.organization.ContractSpecialist;
import com.hope.master_service.entity.AddressEntity;
import com.hope.master_service.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "contract_specialist")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "organization")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ContractSpecialistEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    private String fullName;

    private String email;

    private String primaryOfficePhone;

    @Enumerated(EnumType.STRING)
    private PhoneType phoneType;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private AddressEntity address;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
    }

    public ContractSpecialist toDto() {
        return ContractSpecialist.builder()
                .uuid(this.uuid)
                .fullName(this.fullName)
                .email(this.email)
                .primaryOfficePhone(this.primaryOfficePhone)
                .phoneType(this.phoneType)
                .address(AddressEntity.toDto(this.address))
                .build();
    }

    public static ContractSpecialistEntity fromDto(ContractSpecialist specialist, OrganizationEntity organization) {
        if (specialist == null) return null;
        return ContractSpecialistEntity.builder()
                .organization(organization)
                .fullName(specialist.getFullName())
                .email(specialist.getEmail())
                .primaryOfficePhone(specialist.getPrimaryOfficePhone())
                .phoneType(specialist.getPhoneType())
                .address(AddressEntity.toEntity(specialist.getAddress()))
                .build();
    }
}
