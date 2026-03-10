package com.hope.master_service.dto.organization;

import com.hope.master_service.dto.Address;
import com.hope.master_service.dto.enums.PhoneType;
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
public class OrganizationContact {

    private UUID uuid;

    private boolean primaryContact;

    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^$|^[\\d\\s\\-\\(\\)\\+]+$", message = "Invalid phone number format")
    private String officePhone;

    private String extension;

    private PhoneType phoneType;

    @Pattern(regexp = "^$|^[\\d\\s\\-\\(\\)\\+]+$", message = "Invalid phone number format")
    private String additionalPhone;

    private String additionalExtension;

    private PhoneType additionalPhoneType;

    private Address address;
}
