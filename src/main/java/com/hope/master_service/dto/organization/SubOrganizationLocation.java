package com.hope.master_service.dto.organization;

import com.hope.master_service.dto.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubOrganizationLocation {

    private UUID uuid;

    private Address address;
}
