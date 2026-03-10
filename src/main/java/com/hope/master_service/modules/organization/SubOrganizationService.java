package com.hope.master_service.modules.organization;

import com.hope.master_service.dto.enums.SubOrganizationType;
import com.hope.master_service.dto.organization.SubOrganization;
import com.hope.master_service.dto.organization.SubOrganizationContact;
import com.hope.master_service.dto.organization.SubOrganizationLocation;
import com.hope.master_service.dto.response.ResponseCode;
import com.hope.master_service.exception.HopeException;
import com.hope.master_service.service.AppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class SubOrganizationService extends AppService {

    @Autowired
    private SubOrganizationRepository subOrganizationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    public Page<SubOrganization> search(UUID orgUuid, String search, Boolean active,
                                        List<SubOrganizationType> types, String createdBy,
                                        Instant createdFrom, Instant createdTo,
                                        Pageable pageable) throws HopeException {
        OrganizationEntity org = organizationRepository.findByUuid(orgUuid)
                .orElseThrow(() -> throwException(ResponseCode.ORGANIZATION_NOT_FOUND));
        return subOrganizationRepository.findAll(
                SubOrganizationSpecification.withFilters(org.getId(), search, active, types, createdBy, createdFrom, createdTo),
                pageable
        ).map(SubOrganizationEntity::toDto);
    }

    public SubOrganization getByUuid(UUID uuid) throws HopeException {
        SubOrganizationEntity entity = subOrganizationRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.SUB_ORGANIZATION_NOT_FOUND));
        return entity.toDto();
    }

    @Transactional
    public SubOrganization create(UUID orgUuid, SubOrganization dto) throws HopeException {
        OrganizationEntity org = organizationRepository.findByUuid(orgUuid)
                .orElseThrow(() -> throwException(ResponseCode.ORGANIZATION_NOT_FOUND));

        if (subOrganizationRepository.existsByNameIgnoreCaseAndOrganizationId(dto.getName(), org.getId())) {
            throwError(ResponseCode.SUB_ORGANIZATION_NAME_ALREADY_EXISTS);
        }

        SubOrganizationEntity entity = SubOrganizationEntity.fromDto(dto, org);
        entity = subOrganizationRepository.save(entity);

        // Add contacts
        if (dto.getContacts() != null && !dto.getContacts().isEmpty()) {
            ensurePrimaryContact(dto.getContacts());
            for (SubOrganizationContact contact : dto.getContacts()) {
                entity.getContacts().add(SubOrganizationContactEntity.fromDto(contact, entity));
            }
        }

        // Add locations
        if (dto.getLocations() != null) {
            for (SubOrganizationLocation location : dto.getLocations()) {
                entity.getLocations().add(SubOrganizationLocationEntity.fromDto(location, entity));
            }
        }

        entity = subOrganizationRepository.save(entity);
        log.info("Created sub-organization: {} ({}) under org: {}", entity.getName(), entity.getCode(), org.getName());
        return entity.toDto();
    }

    @Transactional
    public SubOrganization update(UUID uuid, SubOrganization dto) throws HopeException {
        SubOrganizationEntity entity = subOrganizationRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.SUB_ORGANIZATION_NOT_FOUND));

        if (subOrganizationRepository.existsByNameIgnoreCaseAndOrganizationIdAndUuidNot(
                dto.getName(), entity.getOrganization().getId(), uuid)) {
            throwError(ResponseCode.SUB_ORGANIZATION_NAME_ALREADY_EXISTS);
        }

        entity.setName(dto.getName());
        entity.setCode(dto.getCode());
        entity.setType(dto.getType());
        entity.setFiscalYearStart(dto.getFiscalYearStart());
        entity.setFiscalYearEnd(dto.getFiscalYearEnd());
        entity.setPurchaseOrderNumber(dto.getPurchaseOrderNumber());
        entity.setContributionType(dto.getContributionType());
        entity.setContributionValue(dto.getContributionValue());
        entity.setNote(dto.getNote());

        // Update contacts
        updateContacts(entity, dto.getContacts());

        // Update locations
        updateLocations(entity, dto.getLocations());

        entity = subOrganizationRepository.save(entity);
        log.info("Updated sub-organization: {}", entity.getName());
        return entity.toDto();
    }

    @Transactional
    public void updateStatus(UUID uuid, boolean active) throws HopeException {
        SubOrganizationEntity entity = subOrganizationRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.SUB_ORGANIZATION_NOT_FOUND));
        entity.setActive(active);
        subOrganizationRepository.save(entity);
        log.info("Sub-organization {} status set to {}", entity.getName(), active ? "active" : "inactive");
    }

    @Transactional
    public void updateArchiveStatus(UUID uuid, boolean archive) throws HopeException {
        SubOrganizationEntity entity = subOrganizationRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.SUB_ORGANIZATION_NOT_FOUND));
        if (archive) {
            entity.setActive(false);
        }
        entity.setArchive(archive);
        subOrganizationRepository.save(entity);
    }

    private void updateContacts(SubOrganizationEntity entity, List<SubOrganizationContact> contacts) {
        if (entity.getContacts() == null) {
            entity.setContacts(new ArrayList<>());
        }
        entity.getContacts().clear();
        if (contacts != null && !contacts.isEmpty()) {
            ensurePrimaryContact(contacts);
            for (SubOrganizationContact contact : contacts) {
                entity.getContacts().add(SubOrganizationContactEntity.fromDto(contact, entity));
            }
        }
    }

    private void updateLocations(SubOrganizationEntity entity, List<SubOrganizationLocation> locations) {
        if (entity.getLocations() == null) {
            entity.setLocations(new ArrayList<>());
        }
        entity.getLocations().clear();
        if (locations != null) {
            for (SubOrganizationLocation location : locations) {
                entity.getLocations().add(SubOrganizationLocationEntity.fromDto(location, entity));
            }
        }
    }

    private void ensurePrimaryContact(List<SubOrganizationContact> contacts) {
        boolean hasPrimary = contacts.stream().anyMatch(SubOrganizationContact::isPrimaryContact);
        if (!hasPrimary) {
            contacts.get(0).setPrimaryContact(true);
        }
    }
}
