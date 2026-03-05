package com.hope.master_service.modules.provider;

import com.hope.master_service.dto.enums.ProviderType;
import com.hope.master_service.dto.enums.Roles;
import com.hope.master_service.dto.provider.Provider;
import com.hope.master_service.dto.response.ResponseCode;
import com.hope.master_service.dto.user.User;
import com.hope.master_service.exception.HopeException;
import com.hope.master_service.modules.user.UserEntity;
import com.hope.master_service.modules.user.UserService;
import com.hope.master_service.service.AppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class ProviderService extends AppService {

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private UserService userService;

    public Page<Provider> getAll(Pageable pageable) {
        return providerRepository.findByUserArchiveFalse(pageable)
                .map(ProviderEntity::toDto);
    }

    public Provider getByUuid(UUID uuid) throws HopeException {
        ProviderEntity entity = providerRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.NOT_FOUND));
        return entity.toDto();
    }

    @Transactional
    public Provider create(Provider provider) throws HopeException {
        if (provider.getNpi() != null && providerRepository.existsByNpi(provider.getNpi())) {
            throwError(ResponseCode.NPI_ALREADY_EXIST);
        }

        Roles role = provider.getRole() != null ? provider.getRole() : mapProviderTypeToRole(provider.getProviderType());

        User user = User.builder()
                .email(provider.getEmail())
                .firstName(provider.getFirstName())
                .lastName(provider.getLastName())
                .middleName(provider.getMiddleName())
                .phone(provider.getPhone())
                .gender(provider.getGender())
                .jobTitle(provider.getJobTitle())
                .role(role)
                .birthDate(provider.getBirthDate())
                .addressLine1(provider.getAddressLine1())
                .addressLine2(provider.getAddressLine2())
                .city(provider.getCity())
                .state(provider.getState())
                .zipCode(provider.getZipCode())
                .build();

        UserEntity userEntity = userService.createUserEntity(user);

        ProviderEntity entity = ProviderEntity.fromDto(provider, userEntity);
        entity = providerRepository.save(entity);

        log.info("Created provider: {} (type: {}, NPI: {})", userEntity.getEmail(), provider.getProviderType(), provider.getNpi());
        return entity.toDto();
    }

    @Transactional
    public Provider update(UUID uuid, Provider provider) throws HopeException {
        ProviderEntity entity = providerRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.NOT_FOUND));

        if (provider.getNpi() != null && !provider.getNpi().equals(entity.getNpi())
                && providerRepository.existsByNpi(provider.getNpi())) {
            throwError(ResponseCode.NPI_ALREADY_EXIST);
        }

        UserEntity userEntity = entity.getUser();
        userEntity.setFirstName(provider.getFirstName());
        userEntity.setLastName(provider.getLastName());
        userEntity.setMiddleName(provider.getMiddleName());
        userEntity.setPhone(provider.getPhone());
        userEntity.setGender(provider.getGender());
        userEntity.setJobTitle(provider.getJobTitle());
        userEntity.setBirthDate(provider.getBirthDate());
        userEntity.setAddressLine1(provider.getAddressLine1());
        userEntity.setAddressLine2(provider.getAddressLine2());
        userEntity.setCity(provider.getCity());
        userEntity.setState(provider.getState());
        userEntity.setZipCode(provider.getZipCode());

        entity.setProviderType(provider.getProviderType());
        entity.setNpi(provider.getNpi());
        entity.setMedicalLicenseNumber(provider.getMedicalLicenseNumber());
        entity.setLicenseExpiryDate(provider.getLicenseExpiryDate());
        entity.setLicenseState(provider.getLicenseState());
        entity.setElectronicSignature(provider.getElectronicSignature());

        entity = providerRepository.save(entity);
        return entity.toDto();
    }

    @Transactional
    public void updateStatus(UUID uuid, boolean active) throws HopeException {
        ProviderEntity entity = providerRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.NOT_FOUND));
        userService.updateStatus(entity.getUser().getUuid(), active);
    }

    @Transactional
    public void updateArchiveStatus(UUID uuid, boolean archive) throws HopeException {
        ProviderEntity entity = providerRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.NOT_FOUND));
        userService.updateArchiveStatus(entity.getUser().getUuid(), archive);
    }

    private Roles mapProviderTypeToRole(ProviderType providerType) {
        return switch (providerType) {
            case CLINICIAN -> Roles.PROVIDER;
            case STAFF -> Roles.FRONTDESK;
        };
    }
}
