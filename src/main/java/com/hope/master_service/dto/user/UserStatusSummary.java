package com.hope.master_service.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserStatusSummary {

    private long total;
    private long active;
    private long inactive;
    private long pending;
    private long suspended;
}
