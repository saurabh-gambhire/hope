package com.hope.master_service.modules.contract;

import com.hope.master_service.dto.contract.ContractContact;
import com.hope.master_service.dto.enums.ContractContactType;
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
@Table(name = "contract_contact")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "contract")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ContractContactEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private ContractEntity contract;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractContactType contactType;

    private String name;

    private String email;

    private String officePhone;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id")
    private AddressEntity address;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
    }

    public ContractContact toDto() {
        ContractContact dto = ContractContact.builder()
                .uuid(this.uuid)
                .contactType(this.contactType)
                .name(this.name)
                .email(this.email)
                .officePhone(this.officePhone)
                .build();
        if (this.address != null) {
            dto.setAddress(AddressEntity.toDto(this.address));
        }
        return dto;
    }

    public static ContractContactEntity fromDto(ContractContact dto, ContractEntity contract) {
        ContractContactEntity entity = ContractContactEntity.builder()
                .contract(contract)
                .contactType(dto.getContactType())
                .name(dto.getName())
                .email(dto.getEmail())
                .officePhone(dto.getOfficePhone())
                .build();
        if (dto.getAddress() != null) {
            entity.setAddress(AddressEntity.toEntity(dto.getAddress()));
        }
        return entity;
    }
}
