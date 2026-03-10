package com.hope.master_service.dto.homework;

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
public class HomeworkDocumentVersion {

    private UUID uuid;
    private int version;
    private String fileName;
    private long fileSize;
    private String documentUrl;
    private String uploadedBy;
    private Instant uploadedAt;
}
