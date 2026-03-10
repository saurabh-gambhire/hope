package com.hope.master_service.modules.contract;

import com.hope.master_service.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "contract_enrollment")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "contract")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ContractEnrollmentEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private ContractEntity contract;

    @Column(nullable = false)
    private String enrollmentName;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
    }
}
