package com.hope.master_service.modules.contract;

import com.hope.master_service.dto.contract.Contract;
import com.hope.master_service.dto.enums.ContractContactType;
import com.hope.master_service.dto.enums.ContractStatus;
import com.hope.master_service.entity.AuditableEntity;
import com.hope.master_service.modules.organization.OrganizationEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "contract")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"organization", "sourceContract", "terms",
        "subOrganizations", "enrollments", "attendanceStatuses", "contacts", "offerings"})
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ContractEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @Column(nullable = false, unique = true, length = 100)
    private String identifier;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 100)
    private String contractType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal budgetAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @Column(length = 100)
    private String enrollmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContractStatus status;

    private boolean isTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_contract_id")
    private ContractEntity sourceContract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    private boolean active;

    private boolean archive;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startDate ASC")
    private List<ContractTermEntity> terms;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractSubOrganizationEntity> subOrganizations;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractEnrollmentEntity> enrollments;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractAttendanceStatusEntity> attendanceStatuses;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractContactEntity> contacts;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractOfferingEntity> offerings;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
        this.active = true;
        this.archive = false;
        if (this.status == null) {
            this.status = ContractStatus.DRAFT;
        }
        if (this.paidAmount == null) {
            this.paidAmount = BigDecimal.ZERO;
        }
    }

    public boolean hasSubOrganization(Long subOrgId) {
        if (this.subOrganizations == null) return false;
        return this.subOrganizations.stream()
                .anyMatch(cso -> cso.getSubOrganization().getId().equals(subOrgId));
    }

    public Contract toDto() {
        Contract dto = Contract.builder()
                .uuid(this.uuid)
                .identifier(this.identifier)
                .name(this.name)
                .contractType(this.contractType)
                .budgetAmount(this.budgetAmount)
                .paidAmount(this.paidAmount)
                .enrollmentType(this.enrollmentType)
                .status(this.status)
                .isTemplate(this.isTemplate)
                .organizationUuid(this.organization.getUuid())
                .organizationName(this.organization.getName())
                .active(this.active)
                .archive(this.archive)
                .createdBy(this.getCreatedBy())
                .created(this.getCreated())
                .modified(this.getModified())
                .build();

        // Source contract reference
        if (this.sourceContract != null) {
            dto.setSourceContractUuid(this.sourceContract.getUuid());
            dto.setSourceContractName(this.sourceContract.getName());
        }

        // Terms
        if (this.terms != null) {
            dto.setTerms(this.terms.stream()
                    .map(t -> {
                        var termDto = t.toDto();
                        termDto.setTermStatus(computeTermStatus(t.getStartDate(), t.getEndDate()));
                        return termDto;
                    })
                    .collect(Collectors.toList()));
        }

        // Sub-organizations
        if (this.subOrganizations != null) {
            dto.setSubOrganizations(this.subOrganizations.stream()
                    .map(ContractSubOrganizationEntity::toDto)
                    .collect(Collectors.toList()));
            dto.setLinkedSubOrgsCount(this.subOrganizations.size());
        }

        // Enrollments
        if (this.enrollments != null) {
            dto.setEnrollments(this.enrollments.stream()
                    .map(ContractEnrollmentEntity::getEnrollmentName)
                    .collect(Collectors.toList()));
        }

        // Attendance statuses
        if (this.attendanceStatuses != null) {
            dto.setAttendanceStatuses(this.attendanceStatuses.stream()
                    .map(ContractAttendanceStatusEntity::getStatusName)
                    .collect(Collectors.toList()));
        }

        // Contacts
        if (this.contacts != null) {
            this.contacts.stream()
                    .filter(c -> c.getContactType() == ContractContactType.INVOICE_SPECIALIST)
                    .findFirst()
                    .ifPresent(c -> dto.setInvoiceSpecialist(c.toDto()));
            this.contacts.stream()
                    .filter(c -> c.getContactType() == ContractContactType.REPRESENTATIVE)
                    .findFirst()
                    .ifPresent(c -> dto.setContractRepresentative(c.toDto()));
        }

        // Offerings
        if (this.offerings != null) {
            dto.setOfferings(this.offerings.stream()
                    .map(ContractOfferingEntity::toDto)
                    .collect(Collectors.toList()));
        }

        // Computed fields
        computeDates(dto);
        computeRemainingPercentage(dto);

        return dto;
    }

    public Contract toListDto() {
        Contract dto = Contract.builder()
                .uuid(this.uuid)
                .identifier(this.identifier)
                .name(this.name)
                .contractType(this.contractType)
                .budgetAmount(this.budgetAmount)
                .paidAmount(this.paidAmount)
                .enrollmentType(this.enrollmentType)
                .status(this.status)
                .isTemplate(this.isTemplate)
                .active(this.active)
                .archive(this.archive)
                .createdBy(this.getCreatedBy())
                .created(this.getCreated())
                .build();

        if (this.enrollments != null) {
            dto.setEnrollments(this.enrollments.stream()
                    .map(ContractEnrollmentEntity::getEnrollmentName)
                    .collect(Collectors.toList()));
        }

        if (this.attendanceStatuses != null) {
            dto.setAttendanceStatuses(this.attendanceStatuses.stream()
                    .map(ContractAttendanceStatusEntity::getStatusName)
                    .collect(Collectors.toList()));
        }

        if (this.offerings != null) {
            dto.setLinkedSubOrgsCount(this.subOrganizations != null ? this.subOrganizations.size() : 0);
        }

        computeDates(dto);
        computeRemainingPercentage(dto);

        return dto;
    }

    public static ContractEntity fromDto(Contract dto, OrganizationEntity organization) {
        return ContractEntity.builder()
                .identifier(dto.getIdentifier())
                .name(dto.getName())
                .contractType(dto.getContractType())
                .budgetAmount(dto.getBudgetAmount())
                .paidAmount(dto.getPaidAmount() != null ? dto.getPaidAmount() : BigDecimal.ZERO)
                .enrollmentType(dto.getEnrollmentType())
                .status(dto.getStatus() != null ? dto.getStatus() : ContractStatus.DRAFT)
                .isTemplate(dto.isTemplate())
                .organization(organization)
                .terms(new ArrayList<>())
                .subOrganizations(new ArrayList<>())
                .enrollments(new ArrayList<>())
                .attendanceStatuses(new ArrayList<>())
                .contacts(new ArrayList<>())
                .offerings(new ArrayList<>())
                .build();
    }

    private void computeDates(Contract dto) {
        if (this.terms != null && !this.terms.isEmpty()) {
            dto.setContractStartDate(this.terms.stream()
                    .map(ContractTermEntity::getStartDate)
                    .min(Comparator.naturalOrder())
                    .orElse(null));
            dto.setContractEndDate(this.terms.stream()
                    .map(ContractTermEntity::getEndDate)
                    .max(Comparator.naturalOrder())
                    .orElse(null));
        }
    }

    private void computeRemainingPercentage(Contract dto) {
        if (this.budgetAmount != null && this.budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal paid = this.paidAmount != null ? this.paidAmount : BigDecimal.ZERO;
            BigDecimal remaining = this.budgetAmount.subtract(paid);
            dto.setRemainingPercentage(remaining.multiply(BigDecimal.valueOf(100))
                    .divide(this.budgetAmount, 0, RoundingMode.HALF_UP));
        }
    }

    private String computeTermStatus(LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        if (today.isBefore(start)) return "Upcoming";
        if (today.isAfter(end)) return "Expired";
        return "Active";
    }
}
