package com.hope.master_service.dto.organization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Organization {

    private UUID uuid;

    @NotBlank(message = "Organization name is mandatory")
    @Size(max = 255, message = "Name should not exceed {max} characters")
    private String name;

    @NotBlank(message = "Abbreviation is mandatory")
    @Size(max = 50, message = "Abbreviation should not exceed {max} characters")
    private String abbreviation;

    @Size(max = 100, message = "Referrer title should not exceed {max} characters")
    private String referrerTitle;

    @Valid
    private List<OrganizationContact> contacts;

    @Valid
    private BillingContact billingContact;

    @Valid
    private ContractSpecialist contractSpecialist;

    private String note;

    private boolean active;

    private boolean archive;

    private String createdBy;

    private Instant created;
}
