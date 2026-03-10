package com.hope.master_service.dto.contract;

import com.hope.master_service.dto.enums.ContractStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Contract {

    private UUID uuid;

    @NotBlank(message = "Contract identifier is mandatory")
    @Size(max = 100, message = "Identifier should not exceed {max} characters")
    private String identifier;

    @NotBlank(message = "Contract name is mandatory")
    @Size(max = 255, message = "Name should not exceed {max} characters")
    private String name;

    @NotBlank(message = "Contract type is mandatory")
    private String contractType;

    @NotNull(message = "Budget amount is mandatory")
    private BigDecimal budgetAmount;

    private BigDecimal paidAmount;

    private String enrollmentType;

    private ContractStatus status;

    private boolean isTemplate;

    private UUID sourceContractUuid;
    private String sourceContractName;

    @NotNull(message = "Organization is mandatory")
    private UUID organizationUuid;
    private String organizationName;

    // Sub-org association (multi-select)
    @Valid
    private List<ContractSubOrg> subOrganizations;

    private boolean addSubOrgsLater;

    // Terms / Purchase Orders
    @Valid
    private List<ContractTerm> terms;

    // Enrollments
    private List<String> enrollments;

    // Attendance statuses
    private List<String> attendanceStatuses;

    // Contacts
    @Valid
    private ContractContact invoiceSpecialist;

    @Valid
    private ContractContact contractRepresentative;

    // Offerings with rates
    @Valid
    private List<ContractOffering> offerings;

    // Read-only computed fields
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private BigDecimal remainingPercentage;
    private int activeTermIndex;
    private long linkedSubOrgsCount;

    private boolean active;
    private boolean archive;

    private String createdBy;
    private Instant created;
    private Instant modified;
}
