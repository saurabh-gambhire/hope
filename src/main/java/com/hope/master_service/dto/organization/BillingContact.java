package com.hope.master_service.dto.organization;

import com.hope.master_service.dto.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BillingContact {

    private UUID uuid;

    private String name;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^$|^[\\d\\s\\-\\(\\)\\+]+$", message = "Invalid phone number format")
    private String officePhone;

    private Address address;
}
