package com.hope.master_service.dto.homework;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class Homework {

    private UUID uuid;

    @NotBlank(message = "Homework name is mandatory")
    @Size(max = 255, message = "Name should not exceed {max} characters")
    private String name;

    private String content;

    @NotNull(message = "Sub-Organization is mandatory")
    private UUID subOrganizationUuid;

    private String subOrganizationName;

    @NotNull(message = "Contract is mandatory")
    private UUID contractUuid;

    private String contractName;

    // Document fields (read-only, populated on retrieval)
    private String documentFileName;
    private Long documentFileSize;
    private String documentUrl;
    private int currentVersion;

    // Base64-encoded document for upload (write-only)
    private String document;
    private String uploadFileName;
    private Long uploadFileSize;

    private boolean active;
    private boolean archive;

    private List<HomeworkDocumentVersion> documentVersions;

    private String createdBy;
    private Instant created;
}
