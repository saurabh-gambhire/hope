package com.hope.master_service.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hope.master_service.dto.enums.Gender;
import com.hope.master_service.dto.enums.RoleType;
import com.hope.master_service.dto.enums.Roles;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Email;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {

    private UUID uuid;

    private String iamId;

    @Size(min = 5, max = 64, message = "Email length should be between 5-64")
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

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String jobTitle;

    @Enumerated(EnumType.STRING)
    private RoleType roleType;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    private Roles role;

    private Instant birthDate;

    private Instant lastLogin;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean active;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean archive;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean emailVerified;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean phoneVerified;

    // Address fields
    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String zipCode;

    private Map<UUID, String> workLocations;

    private String password;

    private String tenantKey;
}
