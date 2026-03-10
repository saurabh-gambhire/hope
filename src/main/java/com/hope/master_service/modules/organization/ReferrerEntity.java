package com.hope.master_service.modules.organization;

import com.hope.master_service.dto.enums.Gender;
import com.hope.master_service.dto.organization.Referrer;
import com.hope.master_service.entity.AuditableEntity;
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
@Table(name = "referrer")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "organization")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ReferrerEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @Column(nullable = false)
    private String firstName;

    private String middleName;

    @Column(nullable = false)
    private String lastName;

    private String title;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private boolean active;

    private boolean archive;

    @OneToMany(mappedBy = "referrer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReferrerContactEntity> contacts;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
        this.active = true;
        this.archive = false;
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder(firstName);
        if (middleName != null && !middleName.isBlank()) {
            sb.append(" ").append(middleName);
        }
        sb.append(" ").append(lastName);
        return sb.toString();
    }

    public Referrer toDto() {
        return Referrer.builder()
                .uuid(this.uuid)
                .organizationUuid(this.organization.getUuid())
                .organizationName(this.organization.getName())
                .firstName(this.firstName)
                .middleName(this.middleName)
                .lastName(this.lastName)
                .title(this.title)
                .gender(this.gender)
                .active(this.active)
                .archive(this.archive)
                .contacts(this.contacts != null
                        ? this.contacts.stream().map(ReferrerContactEntity::toDto).collect(Collectors.toList())
                        : new ArrayList<>())
                .createdBy(this.getCreatedBy())
                .created(this.getCreated())
                .build();
    }

    public static ReferrerEntity fromDto(Referrer dto, OrganizationEntity organization) {
        return ReferrerEntity.builder()
                .organization(organization)
                .firstName(dto.getFirstName())
                .middleName(dto.getMiddleName())
                .lastName(dto.getLastName())
                .title(dto.getTitle())
                .gender(dto.getGender())
                .contacts(new ArrayList<>())
                .build();
    }
}
