package com.hope.master_service.modules.user;

import com.hope.master_service.dto.enums.Gender;
import com.hope.master_service.dto.enums.RoleType;
import com.hope.master_service.dto.enums.Roles;
import com.hope.master_service.dto.user.User;
import com.hope.master_service.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class UserEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @Column(nullable = false)
    private String iamId;

    @Column(unique = true)
    private String email;

    private String firstName;

    private String lastName;

    private String middleName;

    private String phone;

    private String avatar;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String jobTitle;

    @Enumerated(EnumType.STRING)
    private RoleType roleType;

    @Enumerated(EnumType.STRING)
    private Roles role;

    private Instant lastLogin;

    private Instant birthDate;

    private boolean active;

    private boolean archive;

    private boolean emailVerified;

    private boolean phoneVerified;

    // Address fields
    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String zipCode;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
        this.active = true;
        this.archive = false;
    }

    public User toDto() {
        return User.builder()
                .uuid(this.uuid)
                .iamId(this.iamId)
                .email(this.email)
                .firstName(this.firstName)
                .middleName(this.middleName)
                .lastName(this.lastName)
                .phone(this.phone)
                .avatar(this.avatar)
                .gender(this.gender)
                .jobTitle(this.jobTitle)
                .roleType(this.roleType)
                .role(this.role)
                .lastLogin(this.lastLogin)
                .birthDate(this.birthDate)
                .active(this.active)
                .archive(this.archive)
                .emailVerified(this.emailVerified)
                .phoneVerified(this.phoneVerified)
                .addressLine1(this.addressLine1)
                .addressLine2(this.addressLine2)
                .city(this.city)
                .state(this.state)
                .zipCode(this.zipCode)
                .build();
    }

    public static UserEntity fromDto(User user) {
        return UserEntity.builder()
                .iamId(user.getIamId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .gender(user.getGender())
                .jobTitle(user.getJobTitle())
                .roleType(user.getRoleType())
                .role(user.getRole())
                .birthDate(user.getBirthDate())
                .addressLine1(user.getAddressLine1())
                .addressLine2(user.getAddressLine2())
                .city(user.getCity())
                .state(user.getState())
                .zipCode(user.getZipCode())
                .build();
    }

    public String getName() {
        return firstName + " " + lastName;
    }
}
