package com.hope.master_service.dto.homework;

import jakarta.validation.constraints.NotNull;
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
public class HomeworkCurriculum {

    private UUID uuid;

    @NotNull(message = "Sub-Organization is mandatory")
    private UUID subOrganizationUuid;

    private String subOrganizationName;

    @NotNull(message = "Contract is mandatory")
    private UUID contractUuid;

    private String contractName;

    @NotNull(message = "Homework is mandatory")
    private UUID homeworkUuid;

    private String homeworkName;

    private int sequenceOrder;

    private String createdBy;
    private Instant created;
}
