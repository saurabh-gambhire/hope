package com.hope.master_service.dto.enums;

import lombok.Getter;

@Getter
public enum SubOrganizationType {
    ATTORNEY("Attorney"),
    CHURCH("Church"),
    COURT("Court"),
    PAROLE("Parole"),
    PROBATION("Probation"),
    SOCIAL_SERVICES("Social Services"),
    PRIVATE_PAY("Private Pay"),
    OTHER("Other");

    private final String displayName;

    SubOrganizationType(String displayName) {
        this.displayName = displayName;
    }
}
