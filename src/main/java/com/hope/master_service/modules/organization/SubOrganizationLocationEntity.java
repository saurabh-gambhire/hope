package com.hope.master_service.modules.organization;

import com.hope.master_service.dto.organization.SubOrganizationLocation;
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
@Table(name = "sub_organization_location")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "subOrganization")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class SubOrganizationLocationEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_organization_id", nullable = false)
    private SubOrganizationEntity subOrganization;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private AddressEntity address;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
    }

    public SubOrganizationLocation toDto() {
        return SubOrganizationLocation.builder()
                .uuid(this.uuid)
                .address(AddressEntity.toDto(this.address))
                .build();
    }

    public static SubOrganizationLocationEntity fromDto(SubOrganizationLocation location, SubOrganizationEntity subOrg) {
        return SubOrganizationLocationEntity.builder()
                .subOrganization(subOrg)
                .address(AddressEntity.toEntity(location.getAddress()))
                .build();
    }
}
