package com.hope.master_service.dto.organization;

import com.hope.master_service.dto.enums.ContributionType;
import com.hope.master_service.dto.enums.SubOrganizationType;
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
public class SubOrganization {

    private UUID uuid;

    private UUID organizationUuid;

    private String organizationName;

    @NotBlank(message = "Sub-organization name is mandatory")
    @Size(max = 255, message = "Name should not exceed {max} characters")
    private String name;

    @NotBlank(message = "Sub-organization code is mandatory")
    @Size(max = 50, message = "Code should not exceed {max} characters")
    private String code;

    @NotNull(message = "Sub-organization type is mandatory")
    private SubOrganizationType type;

    private LocalDate fiscalYearStart;

    private LocalDate fiscalYearEnd;

    private String purchaseOrderNumber;

    private ContributionType contributionType;

    private BigDecimal contributionValue;

    @Valid
    private List<SubOrganizationContact> contacts;

    @Valid
    private List<SubOrganizationLocation> locations;

    private String note;

    private boolean active;

    private boolean archive;

    // Aggregated counts (read-only, populated on retrieval)
    private long linkedContracts;
    private long enrolled;
    private long newReferrals;
    private long serviceLocations;
    private long staffCount;
    private long assignedOfferings;
    private long referrerContacts;

    private String createdBy;

    private Instant created;
}
