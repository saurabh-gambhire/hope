package com.hope.master_service.modules.homework;

import com.hope.master_service.dto.homework.HomeworkCurriculum;
import com.hope.master_service.entity.AuditableEntity;
import com.hope.master_service.modules.contract.ContractEntity;
import com.hope.master_service.modules.organization.SubOrganizationEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "homework_curriculum",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_curriculum_sub_org_contract_homework",
                columnNames = {"sub_organization_id", "contract_id", "homework_id"}))
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"subOrganization", "contract", "homework"})
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class HomeworkCurriculumEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_organization_id", nullable = false)
    private SubOrganizationEntity subOrganization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private ContractEntity contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_id", nullable = false)
    private HomeworkEntity homework;

    private int sequenceOrder;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
    }

    public HomeworkCurriculum toDto() {
        return HomeworkCurriculum.builder()
                .uuid(this.uuid)
                .subOrganizationUuid(this.subOrganization.getUuid())
                .subOrganizationName(this.subOrganization.getName())
                .contractUuid(this.contract.getUuid())
                .contractName(this.contract.getName())
                .homeworkUuid(this.homework.getUuid())
                .homeworkName(this.homework.getName())
                .sequenceOrder(this.sequenceOrder)
                .createdBy(this.getCreatedBy())
                .created(this.getCreated())
                .build();
    }
}
