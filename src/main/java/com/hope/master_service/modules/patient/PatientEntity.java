package com.hope.master_service.modules.patient;

import com.hope.master_service.dto.enums.Gender;
import com.hope.master_service.dto.enums.TimeZone;
import com.hope.master_service.dto.patient.Patient;
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
@Table(name = "patient")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PatientEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    private String ssn;

    private String mrn;

    @Enumerated(EnumType.STRING)
    private TimeZone timezone;

    private Instant registrationDate;

    private String faxNumber;

    private String mobileNumber;

    private String homePhone;

    private boolean emailConsent;

    private boolean messageConsent;

    private boolean callConsent;

    private boolean intakeStatus;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private AddressEntity address;

    private static final int MRN_LENGTH = 7;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
    }

    @PostPersist
    public void postPersist() {
        this.mrn = String.format("%0" + MRN_LENGTH + "d", id);
    }

    public Patient toDto() {
        return Patient.builder()
                .uuid(this.uuid)
                .email(this.user.getEmail())
                .firstName(this.user.getFirstName())
                .middleName(this.user.getMiddleName())
                .lastName(this.user.getLastName())
                .phone(this.user.getPhone())
                .avatar(this.user.getAvatar())
                .birthDate(this.user.getBirthDate())
                .active(this.user.isActive())
                .archive(this.user.isArchive())
                .address(AddressEntity.toDto(this.address))
                .gender(this.gender)
                .ssn(this.ssn)
                .mrn(this.mrn)
                .timezone(this.timezone)
                .registrationDate(this.registrationDate)
                .faxNumber(this.faxNumber)
                .mobileNumber(this.mobileNumber)
                .homePhone(this.homePhone)
                .emailConsent(this.emailConsent)
                .messageConsent(this.messageConsent)
                .callConsent(this.callConsent)
                .intakeStatus(this.intakeStatus)
                .build();
    }

    public static PatientEntity fromDto(Patient patient, UserEntity userEntity) {
        return PatientEntity.builder()
                .user(userEntity)
                .gender(patient.getGender())
                .ssn(patient.getSsn())
                .timezone(patient.getTimezone())
                .registrationDate(patient.getRegistrationDate())
                .faxNumber(patient.getFaxNumber())
                .mobileNumber(patient.getMobileNumber())
                .homePhone(patient.getHomePhone())
                .emailConsent(patient.isEmailConsent())
                .messageConsent(patient.isMessageConsent())
                .callConsent(patient.isCallConsent())
                .address(AddressEntity.toEntity(patient.getAddress()))
                .build();
    }
}
