package com.hope.master_service.dto.patient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hope.master_service.dto.Address;
import com.hope.master_service.dto.enums.Gender;
import com.hope.master_service.dto.enums.TimeZone;
import jakarta.validation.Valid;
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
public class Patient {

    private UUID uuid;

    // User fields
    @NotBlank(message = "Email is mandatory")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "First Name is mandatory")
    @Size(min = 2, max = 32, message = "First Name length should be between 2-32")
    private String firstName;

    @NotBlank(message = "Last Name is mandatory")
    @Size(min = 2, max = 32, message = "Last Name length should be between 2-32")
    private String lastName;

    private String middleName;

    @NotBlank(message = "Phone Number is mandatory")
    private String phone;

    private String avatar;

    private Instant birthDate;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean active;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean archive;

    @Valid
    private Address address;

    // Patient fields
    @NotNull(message = "Gender is mandatory")
    private Gender gender;

    private String ssn;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String mrn;

    private TimeZone timezone;

    private Instant registrationDate;

    private String faxNumber;

    private String mobileNumber;

    private String homePhone;

    private boolean emailConsent;

    private boolean messageConsent;

    private boolean callConsent;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean intakeStatus;

    private String password;
}
