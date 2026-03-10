package com.hope.master_service.modules.organization;

import com.hope.master_service.dto.organization.Organization;
import com.hope.master_service.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "organization")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class OrganizationEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String abbreviation;

    private String referrerTitle;

    @Column(columnDefinition = "TEXT")
    private String note;

    private boolean active;

    private boolean archive;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrganizationContactEntity> contacts = new ArrayList<>();

    @OneToOne(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private BillingContactEntity billingContact;

    @OneToOne(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private ContractSpecialistEntity contractSpecialist;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
        this.active = true;
        this.archive = false;
    }

    public Organization toDto() {
        return Organization.builder()
                .uuid(this.uuid)
                .name(this.name)
                .abbreviation(this.abbreviation)
                .referrerTitle(this.referrerTitle)
                .note(this.note)
                .active(this.active)
                .archive(this.archive)
                .contacts(this.contacts != null
                        ? this.contacts.stream().map(OrganizationContactEntity::toDto).collect(Collectors.toList())
                        : new ArrayList<>())
                .billingContact(this.billingContact != null ? this.billingContact.toDto() : null)
                .contractSpecialist(this.contractSpecialist != null ? this.contractSpecialist.toDto() : null)
                .createdBy(this.getCreatedBy())
                .created(this.getCreated())
                .build();
    }

    public static OrganizationEntity fromDto(Organization org) {
        return OrganizationEntity.builder()
                .name(org.getName())
                .abbreviation(org.getAbbreviation())
                .referrerTitle(org.getReferrerTitle())
                .note(org.getNote())
                .contacts(new ArrayList<>())
                .build();
    }
}
