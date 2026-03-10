package com.hope.master_service.entity;

import com.hope.master_service.dto.Address;
import com.hope.master_service.dto.enums.USState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "address")
@SuperBuilder
public class AddressEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    private String line1;

    private String line2;

    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "state_code")
    private USState state;

    private String country;

    private String zipcode;

    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID();
    }

    public static Address toDto(AddressEntity addressEntity) {
        if (addressEntity == null) return null;
        return Address.builder()
                .uuid(addressEntity.getUuid())
                .line1(addressEntity.getLine1())
                .line2(addressEntity.getLine2())
                .city(addressEntity.getCity())
                .state(addressEntity.getState())
                .country(addressEntity.getCountry())
                .zipcode(addressEntity.getZipcode())
                .build();
    }

    public static AddressEntity toEntity(Address address) {
        if (address == null) return null;
        return AddressEntity.builder()
                .uuid(Objects.isNull(address.getUuid()) ? UUID.randomUUID() : address.getUuid())
                .line1(address.getLine1())
                .line2(address.getLine2())
                .city(address.getCity())
                .state(address.getState())
                .country(address.getCountry())
                .zipcode(address.getZipcode())
                .build();
    }

    public static AddressEntity updateEntity(AddressEntity addressEntity, Address address) {
        if (address == null) return addressEntity;
        if (addressEntity == null) return toEntity(address);
        addressEntity.setLine1(address.getLine1());
        addressEntity.setLine2(address.getLine2());
        addressEntity.setCity(address.getCity());
        addressEntity.setState(address.getState());
        addressEntity.setCountry(address.getCountry());
        addressEntity.setZipcode(address.getZipcode());
        addressEntity.setModified(Instant.now());
        return addressEntity;
    }
}
