package com.hope.master_service.dto.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContractTerm {

    private UUID uuid;

    @NotBlank(message = "Purchase order number is mandatory")
    private String purchaseOrderNumber;

    @NotNull(message = "Term start date is mandatory")
    private LocalDate startDate;

    @NotNull(message = "Term end date is mandatory")
    private LocalDate endDate;

    private String termStatus;
}
