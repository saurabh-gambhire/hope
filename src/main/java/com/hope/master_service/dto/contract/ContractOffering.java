package com.hope.master_service.dto.contract;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContractOffering {

    private UUID uuid;

    @NotBlank(message = "Offering name is mandatory")
    private String offeringName;

    private String serviceCode;

    private BigDecimal defaultRate;

    private BigDecimal templateRate;

    private boolean active;
}
