package com.hope.master_service.modules.homework;

import com.hope.master_service.dto.homework.Homework;
import com.hope.master_service.entity.AuditableEntity;
import com.hope.master_service.modules.contract.ContractEntity;
import com.hope.master_service.modules.organization.SubOrganizationEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "homework")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"subOrganization", "contract", "documentVersions"})
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class HomeworkEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_organization_id", nullable = false)
    private SubOrganizationEntity subOrganization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private ContractEntity contract;

    private String documentFileName;

    private Long documentFileSize;

    @Column(name = "document_s3_key", length = 500)
    private String documentS3Key;

    @Column(columnDefinition = "INT DEFAULT 0")
    private int currentVersion;

    private boolean active;

    private boolean archive;

    @OneToMany(mappedBy = "homework", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("version DESC")
    private List<HomeworkDocumentVersionEntity> documentVersions;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
        this.active = true;
        this.archive = false;
        this.currentVersion = 0;
    }

    public Homework toDto() {
        return Homework.builder()
                .uuid(this.uuid)
                .name(this.name)
                .content(this.content)
                .subOrganizationUuid(this.subOrganization.getUuid())
                .subOrganizationName(this.subOrganization.getName())
                .contractUuid(this.contract.getUuid())
                .contractName(this.contract.getName())
                .documentFileName(this.documentFileName)
                .documentFileSize(this.documentFileSize)
                .currentVersion(this.currentVersion)
                .active(this.active)
                .archive(this.archive)
                .documentVersions(this.documentVersions != null
                        ? this.documentVersions.stream().map(HomeworkDocumentVersionEntity::toDto).collect(Collectors.toList())
                        : new ArrayList<>())
                .createdBy(this.getCreatedBy())
                .created(this.getCreated())
                .build();
    }

    public Homework toListDto() {
        return Homework.builder()
                .uuid(this.uuid)
                .name(this.name)
                .subOrganizationUuid(this.subOrganization.getUuid())
                .subOrganizationName(this.subOrganization.getName())
                .contractUuid(this.contract.getUuid())
                .contractName(this.contract.getName())
                .documentFileName(this.documentFileName)
                .active(this.active)
                .archive(this.archive)
                .createdBy(this.getCreatedBy())
                .created(this.getCreated())
                .build();
    }

    public static HomeworkEntity fromDto(Homework dto, SubOrganizationEntity subOrganization, ContractEntity contract) {
        return HomeworkEntity.builder()
                .name(dto.getName())
                .content(dto.getContent())
                .subOrganization(subOrganization)
                .contract(contract)
                .documentVersions(new ArrayList<>())
                .build();
    }
}
