package com.hope.master_service.modules.provider;

import com.hope.master_service.dto.enums.ProviderType;
import com.hope.master_service.dto.provider.Provider;
import com.hope.master_service.entity.AddressEntity;
import com.hope.master_service.entity.AuditableEntity;
import com.hope.master_service.modules.user.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ProviderEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType providerType;

    @Column(unique = true)
    private Long npi;

    // Credentials (clinician-specific)
    private String medicalLicenseNumber;

    private Instant licenseExpiryDate;

    private String licenseState;

    private String electronicSignature;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
    }

    public Provider toDto() {
        return Provider.builder()
                .uuid(this.uuid)
                .email(this.user.getEmail())
                .firstName(this.user.getFirstName())
                .middleName(this.user.getMiddleName())
                .lastName(this.user.getLastName())
                .phone(this.user.getPhone())
                .avatar(this.user.getAvatar())
                .gender(this.user.getGender())
                .role(this.user.getRole())
                .roleType(this.user.getRoleType())
                .birthDate(this.user.getBirthDate())
                .active(this.user.isActive())
                .archive(this.user.isArchive())
                .jobTitle(this.user.getJobTitle())
                .address(AddressEntity.toDto(this.user.getAddress()))
                .providerType(this.providerType)
                .npi(this.npi)
                .medicalLicenseNumber(this.medicalLicenseNumber)
                .licenseExpiryDate(this.licenseExpiryDate)
                .licenseState(this.licenseState)
                .electronicSignature(this.electronicSignature)
                .build();
    }

    public static ProviderEntity fromDto(Provider provider, UserEntity userEntity) {
        return ProviderEntity.builder()
                .user(userEntity)
                .providerType(provider.getProviderType())
                .npi(provider.getNpi())
                .medicalLicenseNumber(provider.getMedicalLicenseNumber())
                .licenseExpiryDate(provider.getLicenseExpiryDate())
                .licenseState(provider.getLicenseState())
                .build();
    }
}
