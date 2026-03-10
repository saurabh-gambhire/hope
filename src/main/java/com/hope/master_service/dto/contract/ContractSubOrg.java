package com.hope.master_service.dto.contract;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContractSubOrg {

    private UUID uuid;

    @NotNull(message = "Sub-organization is mandatory")
    private UUID subOrganizationUuid;

    private String subOrganizationName;
}
