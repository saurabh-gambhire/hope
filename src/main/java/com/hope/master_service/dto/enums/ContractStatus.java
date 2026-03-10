package com.hope.master_service.dto.enums;

import lombok.Getter;

@Getter
public enum ContractStatus {
    DRAFT("Draft"),
    ACTIVE("Active"),
    EXPIRED("Expired"),
    SUSPENDED("Suspended");

    private final String displayName;

    ContractStatus(String displayName) {
        this.displayName = displayName;
    }
}
