package com.hope.master_service.dto.contract;

import com.hope.master_service.dto.Address;
import com.hope.master_service.dto.enums.ContractContactType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContractContact {

    private UUID uuid;

    private ContractContactType contactType;

    private String name;

    @Email(message = "Invalid email format")
    private String email;

    private String officePhone;

    @Valid
    private Address address;
}
