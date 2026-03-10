package com.hope.master_service.modules.organization;

import com.hope.master_service.dto.enums.PhoneType;
import com.hope.master_service.dto.organization.OrganizationContact;
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
@Table(name = "organization_contact")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "organization")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class OrganizationContactEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @Column(name = "is_primary")
    private boolean primaryContact;

    private String fullName;

    private String email;

    private String officePhone;

    private String extension;

    @Enumerated(EnumType.STRING)
    private PhoneType phoneType;

    private String additionalPhone;

    private String additionalExtension;

    @Enumerated(EnumType.STRING)
    private PhoneType additionalPhoneType;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private AddressEntity address;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
    }

    public OrganizationContact toDto() {
        return OrganizationContact.builder()
                .uuid(this.uuid)
                .primaryContact(this.primaryContact)
                .fullName(this.fullName)
                .email(this.email)
                .officePhone(this.officePhone)
                .extension(this.extension)
                .phoneType(this.phoneType)
                .additionalPhone(this.additionalPhone)
                .additionalExtension(this.additionalExtension)
                .additionalPhoneType(this.additionalPhoneType)
                .address(AddressEntity.toDto(this.address))
                .build();
    }

    public static OrganizationContactEntity fromDto(OrganizationContact contact, OrganizationEntity organization) {
        return OrganizationContactEntity.builder()
                .organization(organization)
                .primaryContact(contact.isPrimaryContact())
                .fullName(contact.getFullName())
                .email(contact.getEmail())
                .officePhone(contact.getOfficePhone())
                .extension(contact.getExtension())
                .phoneType(contact.getPhoneType())
                .additionalPhone(contact.getAdditionalPhone())
                .additionalExtension(contact.getAdditionalExtension())
                .additionalPhoneType(contact.getAdditionalPhoneType())
                .address(AddressEntity.toEntity(contact.getAddress()))
                .build();
    }
}
