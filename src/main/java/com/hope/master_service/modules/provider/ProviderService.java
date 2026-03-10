package com.hope.master_service.modules.provider;

import com.hope.master_service.dto.enums.ProviderType;
import com.hope.master_service.dto.enums.Roles;
import com.hope.master_service.dto.enums.UserStatus;
import com.hope.master_service.dto.provider.Provider;
import com.hope.master_service.dto.response.ResponseCode;
import com.hope.master_service.dto.user.User;
import com.hope.master_service.dto.user.UserStatusSummary;
import com.hope.master_service.entity.AddressEntity;
import com.hope.master_service.exception.HopeException;
import com.hope.master_service.modules.user.UserEntity;
import com.hope.master_service.modules.user.UserService;
import com.hope.master_service.service.AppService;
import com.hope.master_service.service.AwsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ProviderService extends AppService {

    private static final String SIGNATURE_FOLDER = "electronic-signatures";

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AwsService awsService;

    public Page<Provider> getAll(Pageable pageable) throws HopeException {
        Page<Provider> providers = providerRepository.findByUserArchiveFalse(pageable)
                .map(ProviderEntity::toDto);
        for (Provider provider : providers.getContent()) {
            resolveSignatureUrl(provider);
        }
        return providers;
    }

    public Page<Provider> search(String search, UserStatus status, List<Roles> roles,
                                 ProviderType providerType, Instant lastLoginFrom,
                                 Instant lastLoginTo, Boolean neverLoggedIn,
                                 Pageable pageable) throws HopeException {
        Page<Provider> providers = providerRepository.findAll(
                ProviderSpecification.withFilters(search, status, roles, providerType,
                        lastLoginFrom, lastLoginTo, neverLoggedIn),
                pageable
        ).map(ProviderEntity::toDto);
        for (Provider provider : providers.getContent()) {
            resolveSignatureUrl(provider);
        }
        return providers;
    }

    public UserStatusSummary getStatusCounts() {
        long total = providerRepository.count();
        long active = providerRepository.count(ProviderSpecification.isActive());
        long inactive = providerRepository.count(ProviderSpecification.isInactive());
        long pending = providerRepository.count(ProviderSpecification.isPending());
        long suspended = providerRepository.count(ProviderSpecification.isSuspended());

        return UserStatusSummary.builder()
                .total(total)
                .active(active)
                .inactive(inactive)
                .pending(pending)
                .suspended(suspended)
                .build();
    }

    public Provider getByUuid(UUID uuid) throws HopeException {
        ProviderEntity entity = providerRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.NOT_FOUND));
        Provider provider = entity.toDto();
        resolveSignatureUrl(provider);
        return provider;
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
                .address(provider.getAddress())
                .build();

        UserEntity userEntity = userService.createUserEntity(user);

        // Upload electronic signature to S3 if provided
        String signatureKey = uploadSignature(provider.getElectronicSignature(), userEntity.getUuid());

        ProviderEntity entity = ProviderEntity.fromDto(provider, userEntity);
        entity.setElectronicSignature(signatureKey);
        entity = providerRepository.save(entity);

        log.info("Created provider: {} (type: {}, NPI: {})", userEntity.getEmail(), provider.getProviderType(), provider.getNpi());

        Provider result = entity.toDto();
        resolveSignatureUrl(result);
        return result;
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
        userEntity.setAddress(AddressEntity.updateEntity(userEntity.getAddress(), provider.getAddress()));

        entity.setProviderType(provider.getProviderType());
        entity.setNpi(provider.getNpi());
        entity.setMedicalLicenseNumber(provider.getMedicalLicenseNumber());
        entity.setLicenseExpiryDate(provider.getLicenseExpiryDate());
        entity.setLicenseState(provider.getLicenseState());

        // Update electronic signature if new base64 is provided
        if (StringUtils.isNotBlank(provider.getElectronicSignature())) {
            // Delete old signature from S3
            if (StringUtils.isNotBlank(entity.getElectronicSignature())) {
                awsService.deleteObject(entity.getElectronicSignature());
            }
            String signatureKey = uploadSignature(provider.getElectronicSignature(), entity.getUuid());
            entity.setElectronicSignature(signatureKey);
        }

        entity = providerRepository.save(entity);

        Provider result = entity.toDto();
        resolveSignatureUrl(result);
        return result;
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

    private String uploadSignature(String base64Signature, UUID providerUuid) throws HopeException {
        if (StringUtils.isBlank(base64Signature)) {
            return null;
        }
        return awsService.uploadBase64(base64Signature, SIGNATURE_FOLDER, providerUuid);
    }

    private void resolveSignatureUrl(Provider provider) throws HopeException {
        if (StringUtils.isNotBlank(provider.getElectronicSignature())) {
            String preSignedUrl = awsService.getPreSignedUrl(provider.getElectronicSignature());
            provider.setElectronicSignature(preSignedUrl);
        }
    }

    private Roles mapProviderTypeToRole(ProviderType providerType) {
        return switch (providerType) {
            case CLINICIAN -> Roles.PROVIDER;
            case STAFF -> Roles.FRONTDESK;
        };
    }
}
