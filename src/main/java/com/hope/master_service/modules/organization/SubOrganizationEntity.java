package com.hope.master_service.modules.organization;

import com.hope.master_service.dto.enums.ContributionType;
import com.hope.master_service.dto.enums.SubOrganizationType;
import com.hope.master_service.dto.organization.SubOrganization;
import com.hope.master_service.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "sub_organization")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "organization")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class SubOrganizationEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubOrganizationType type;

    private LocalDate fiscalYearStart;

    private LocalDate fiscalYearEnd;

    private String purchaseOrderNumber;

    @Enumerated(EnumType.STRING)
    private ContributionType contributionType;

    @Column(precision = 12, scale = 2)
    private BigDecimal contributionValue;

    @Column(columnDefinition = "TEXT")
    private String note;

    private boolean active;

    private boolean archive;

    @OneToMany(mappedBy = "subOrganization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubOrganizationContactEntity> contacts;

    @OneToMany(mappedBy = "subOrganization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubOrganizationLocationEntity> locations;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
        this.active = true;
        this.archive = false;
    }

    public SubOrganization toDto() {
        return SubOrganization.builder()
                .uuid(this.uuid)
                .organizationUuid(this.organization.getUuid())
                .organizationName(this.organization.getName())
                .name(this.name)
                .code(this.code)
                .type(this.type)
                .fiscalYearStart(this.fiscalYearStart)
                .fiscalYearEnd(this.fiscalYearEnd)
                .purchaseOrderNumber(this.purchaseOrderNumber)
                .contributionType(this.contributionType)
                .contributionValue(this.contributionValue)
                .note(this.note)
                .active(this.active)
                .archive(this.archive)
                .contacts(this.contacts != null
                        ? this.contacts.stream().map(SubOrganizationContactEntity::toDto).collect(Collectors.toList())
                        : new ArrayList<>())
                .locations(this.locations != null
                        ? this.locations.stream().map(SubOrganizationLocationEntity::toDto).collect(Collectors.toList())
                        : new ArrayList<>())
                .serviceLocations(this.locations != null ? this.locations.size() : 0)
                .createdBy(this.getCreatedBy())
                .created(this.getCreated())
                .build();
    }

    public static SubOrganizationEntity fromDto(SubOrganization dto, OrganizationEntity organization) {
        return SubOrganizationEntity.builder()
                .organization(organization)
                .name(dto.getName())
                .code(dto.getCode())
                .type(dto.getType())
                .fiscalYearStart(dto.getFiscalYearStart())
                .fiscalYearEnd(dto.getFiscalYearEnd())
                .purchaseOrderNumber(dto.getPurchaseOrderNumber())
                .contributionType(dto.getContributionType())
                .contributionValue(dto.getContributionValue())
                .note(dto.getNote())
                .contacts(new ArrayList<>())
                .locations(new ArrayList<>())
                .build();
    }
}
