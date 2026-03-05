package com.hope.master_service.dto.enums;

import java.util.Arrays;
import java.util.List;

public enum Roles {

    SUPER_ADMIN,

    PROVIDER,

    FRONTDESK,

    BILLER,

    ENB,

    PSYCHIATRIST,

    THERAPIST,

    NURSE,

    PATIENT,

    ANONYMOUS,

    RESOLUTION_SPECIALIST,

    PROVIDER_GROUP_ADMIN;


    private Roles() {
    }

    public static List<Roles> getStaffRoles() {
        return Arrays.asList(SUPER_ADMIN, FRONTDESK, BILLER, ENB, RESOLUTION_SPECIALIST);
    }

    public static List<Roles> getProviderRoles() {
        return Arrays.asList(PSYCHIATRIST, THERAPIST, NURSE, PROVIDER);
    }

}
