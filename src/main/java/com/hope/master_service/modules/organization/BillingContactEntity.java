package com.hope.master_service.modules.organization;

import com.hope.master_service.dto.organization.BillingContact;
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
@Table(name = "billing_contact")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "organization")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class BillingContactEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    private String name;

    private String email;

    private String officePhone;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private AddressEntity address;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
    }

    public BillingContact toDto() {
        return BillingContact.builder()
                .uuid(this.uuid)
                .name(this.name)
                .email(this.email)
                .officePhone(this.officePhone)
                .address(AddressEntity.toDto(this.address))
                .build();
    }

    public static BillingContactEntity fromDto(BillingContact billing, OrganizationEntity organization) {
        if (billing == null) return null;
        return BillingContactEntity.builder()
                .organization(organization)
                .name(billing.getName())
                .email(billing.getEmail())
                .officePhone(billing.getOfficePhone())
                .address(AddressEntity.toEntity(billing.getAddress()))
                .build();
    }
}
