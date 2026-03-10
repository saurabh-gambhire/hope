package com.hope.master_service.dto.organization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrganizationOverview {

    private Organization organization;

    // KPI counts
    private long totalSubOrgContract;
    private long totalContractsCount;
    private long newReferrals;
    private long enrolled;
    private long totalStaffCount;
    private long assignedOfferings;
    private long referrerContacts;

    // Summary lists for overview sections
    private List<SubOrganization> subOrganizations;
    private List<Referrer> referrers;
}
