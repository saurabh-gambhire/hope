package com.hope.master_service.dto.organization;

import com.hope.master_service.dto.Address;
import com.hope.master_service.dto.enums.PhoneType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReferrerContact {

    private UUID uuid;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^$|^[\\d\\s\\-\\(\\)\\+]+$", message = "Invalid phone number format")
    private String primaryPhone;

    private String extension;

    private PhoneType phoneType;

    private boolean notOkToLeaveMessage;

    private Address address;
}
