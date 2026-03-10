package com.hope.master_service.modules.organization;

import com.hope.master_service.dto.enums.PhoneType;
import com.hope.master_service.dto.organization.ReferrerContact;
import com.hope.master_service.entity.AddressEntity;
import com.hope.master_service.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "referrer_contact")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "referrer")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ReferrerContactEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_id", nullable = false)
    private ReferrerEntity referrer;

    private String email;

    private String primaryPhone;

    private String extension;

    @Enumerated(EnumType.STRING)
    private PhoneType phoneType;

    private boolean notOkToLeaveMessage;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private AddressEntity address;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
    }

    public ReferrerContact toDto() {
        return ReferrerContact.builder()
                .uuid(this.uuid)
                .email(this.email)
                .primaryPhone(this.primaryPhone)
                .extension(this.extension)
                .phoneType(this.phoneType)
                .notOkToLeaveMessage(this.notOkToLeaveMessage)
                .address(AddressEntity.toDto(this.address))
                .build();
    }

    public static ReferrerContactEntity fromDto(ReferrerContact contact, ReferrerEntity referrer) {
        return ReferrerContactEntity.builder()
                .referrer(referrer)
                .email(contact.getEmail())
                .primaryPhone(contact.getPrimaryPhone())
                .extension(contact.getExtension())
                .phoneType(contact.getPhoneType())
                .notOkToLeaveMessage(contact.isNotOkToLeaveMessage())
                .address(AddressEntity.toEntity(contact.getAddress()))
                .build();
    }
}
