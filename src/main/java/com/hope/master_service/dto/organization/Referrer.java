package com.hope.master_service.dto.organization;

import com.hope.master_service.dto.Address;
import com.hope.master_service.dto.enums.Gender;
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
public class Referrer {

    private UUID uuid;

    private UUID organizationUuid;

    private String organizationName;

    @NotBlank(message = "First name is mandatory")
    @Size(max = 100, message = "First name should not exceed {max} characters")
    private String firstName;

    @Size(max = 100, message = "Middle name should not exceed {max} characters")
    private String middleName;

    @NotBlank(message = "Last name is mandatory")
    @Size(max = 100, message = "Last name should not exceed {max} characters")
    private String lastName;

    @Size(max = 50, message = "Title should not exceed {max} characters")
    private String title;

    private Gender gender;

    // Consolidated address for list view display
    private Address orgAddress;
    private Address refAddress;

    @Valid
    private List<ReferrerContact> contacts;

    private boolean active;

    private boolean archive;

    private String createdBy;

    private Instant created;
}
