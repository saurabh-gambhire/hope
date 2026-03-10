package com.hope.master_service.modules.homework;

import com.hope.master_service.dto.homework.HomeworkDocumentVersion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "homework_document_version")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HomeworkDocumentVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_id", nullable = false)
    private HomeworkEntity homework;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private long fileSize;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    private String uploadedBy;

    @Column(nullable = false)
    private Instant uploadedAt;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
        if (this.uploadedAt == null) {
            this.uploadedAt = Instant.now();
        }
    }

    public HomeworkDocumentVersion toDto() {
        return HomeworkDocumentVersion.builder()
                .uuid(this.uuid)
                .version(this.version)
                .fileName(this.fileName)
                .fileSize(this.fileSize)
                .uploadedBy(this.uploadedBy)
                .uploadedAt(this.uploadedAt)
                .build();
    }
}
