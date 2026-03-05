package com.hope.master_service.dto.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hope.master_service.dto.enums.Gender;
import com.hope.master_service.dto.enums.ProviderType;
import com.hope.master_service.dto.enums.RoleType;
import com.hope.master_service.dto.enums.Roles;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Provider {

    private UUID uuid;

    // User fields
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "First Name is required")
    @Size(min = 2, max = 32, message = "First Name length should be between 2-32")
    private String firstName;

    @NotBlank(message = "Last Name is required")
    @Size(min = 2, max = 32, message = "Last Name length should be between 2-32")
    private String lastName;

    private String middleName;

    @NotBlank(message = "Phone Number is required")
    private String phone;

    private String avatar;

    private Gender gender;

    private Roles role;

    private RoleType roleType;

    private Instant birthDate;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean active;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean archive;

    private String jobTitle;

    // Address fields
    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String zipCode;

    // Provider fields
    @NotNull(message = "Provider type is required")
    private ProviderType providerType;

    private Long npi;

    // Credentials (clinician-specific)
    private String medicalLicenseNumber;

    private Instant licenseExpiryDate;

    private String licenseState;

    private String electronicSignature;

    private String password;
}
