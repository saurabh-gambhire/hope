package com.hope.master_service.modules.organization;

import com.hope.master_service.dto.organization.Referrer;
import com.hope.master_service.dto.organization.ReferrerContact;
import com.hope.master_service.dto.response.ResponseCode;
import com.hope.master_service.exception.HopeException;
import com.hope.master_service.service.AppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ReferrerService extends AppService {

    @Autowired
    private ReferrerRepository referrerRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    public Page<Referrer> search(UUID orgUuid, String search, Pageable pageable) throws HopeException {
        OrganizationEntity org = organizationRepository.findByUuid(orgUuid)
                .orElseThrow(() -> throwException(ResponseCode.ORGANIZATION_NOT_FOUND));
        return referrerRepository.findAll(
                ReferrerSpecification.withFilters(org.getId(), search),
                pageable
        ).map(ReferrerEntity::toDto);
    }

    public Referrer getByUuid(UUID uuid) throws HopeException {
        ReferrerEntity entity = referrerRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.REFERRER_NOT_FOUND));
        return entity.toDto();
    }

    @Transactional
    public Referrer create(UUID orgUuid, Referrer dto) throws HopeException {
        OrganizationEntity org = organizationRepository.findByUuid(orgUuid)
                .orElseThrow(() -> throwException(ResponseCode.ORGANIZATION_NOT_FOUND));

        ReferrerEntity entity = ReferrerEntity.fromDto(dto, org);
        entity = referrerRepository.save(entity);

        // Add contacts
        if (dto.getContacts() != null) {
            for (ReferrerContact contact : dto.getContacts()) {
                entity.getContacts().add(ReferrerContactEntity.fromDto(contact, entity));
            }
            entity = referrerRepository.save(entity);
        }

        log.info("Created referrer: {} under org: {}", entity.getFullName(), org.getName());
        return entity.toDto();
    }

    @Transactional
    public Referrer update(UUID uuid, Referrer dto) throws HopeException {
        ReferrerEntity entity = referrerRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.REFERRER_NOT_FOUND));

        entity.setFirstName(dto.getFirstName());
        entity.setMiddleName(dto.getMiddleName());
        entity.setLastName(dto.getLastName());
        entity.setTitle(dto.getTitle());
        entity.setGender(dto.getGender());

        // Update contacts
        if (entity.getContacts() == null) {
            entity.setContacts(new ArrayList<>());
        }
        entity.getContacts().clear();
        if (dto.getContacts() != null) {
            for (ReferrerContact contact : dto.getContacts()) {
                entity.getContacts().add(ReferrerContactEntity.fromDto(contact, entity));
            }
        }

        entity = referrerRepository.save(entity);
        log.info("Updated referrer: {}", entity.getFullName());
        return entity.toDto();
    }

    @Transactional
    public void updateArchiveStatus(UUID uuid, boolean archive) throws HopeException {
        ReferrerEntity entity = referrerRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.REFERRER_NOT_FOUND));
        if (archive) {
            entity.setActive(false);
        }
        entity.setArchive(archive);
        referrerRepository.save(entity);
    }
}
