package com.hope.master_service.modules.organization;

import com.hope.master_service.dto.organization.*;
import com.hope.master_service.dto.response.ResponseCode;
import com.hope.master_service.entity.AddressEntity;
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
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrganizationService extends AppService {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SubOrganizationRepository subOrganizationRepository;

    @Autowired
    private ReferrerRepository referrerRepository;

    public Page<Organization> getAll(Pageable pageable) {
        return organizationRepository.findByArchiveFalse(pageable)
                .map(OrganizationEntity::toDto);
    }

    public Page<Organization> getArchived(Pageable pageable) {
        return organizationRepository.findByArchiveTrue(pageable)
                .map(OrganizationEntity::toDto);
    }

    public Page<Organization> search(String search, Pageable pageable) {
        return organizationRepository.findAll(
                OrganizationSpecification.withFilters(search),
                pageable
        ).map(OrganizationEntity::toDto);
    }

    public Organization getByUuid(UUID uuid) throws HopeException {
        OrganizationEntity entity = organizationRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.NOT_FOUND));
        return entity.toDto();
    }

    public OrganizationOverview getOverview(UUID uuid) throws HopeException {
        OrganizationEntity entity = organizationRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.NOT_FOUND));

        Organization org = entity.toDto();

        long subOrgCount = subOrganizationRepository.countByOrganizationIdAndArchiveFalseAndActiveTrue(entity.getId());
        long referrerCount = referrerRepository.countByOrganizationIdAndArchiveFalseAndActiveTrue(entity.getId());

        List<SubOrganization> subOrgs = subOrganizationRepository
                .findByOrganizationIdAndArchiveFalseAndActiveTrue(entity.getId())
                .stream().map(SubOrganizationEntity::toDto).collect(Collectors.toList());

        List<Referrer> referrers = referrerRepository
                .findByOrganizationIdAndArchiveFalseAndActiveTrue(entity.getId())
                .stream().map(ReferrerEntity::toDto).collect(Collectors.toList());

        return OrganizationOverview.builder()
                .organization(org)
                .totalSubOrgContract(subOrgCount)
                .totalContractsCount(0) // TODO: populate when contracts module is built
                .newReferrals(0) // TODO: populate when referral tracking is built
                .enrolled(0) // TODO: populate when enrollment module is built
                .totalStaffCount(0) // TODO: populate when staff assignment module is built
                .assignedOfferings(0) // TODO: populate when offerings module is built
                .referrerContacts(referrerCount)
                .subOrganizations(subOrgs)
                .referrers(referrers)
                .build();
    }

    @Transactional
    public Organization create(Organization organization) throws HopeException {
        if (organizationRepository.existsByNameIgnoreCase(organization.getName())) {
            throwError(ResponseCode.ORGANIZATION_NAME_ALREADY_EXISTS);
        }

        OrganizationEntity entity = OrganizationEntity.fromDto(organization);
        entity = organizationRepository.save(entity);

        // Add contacts — ensure first contact is marked as primary
        if (organization.getContacts() != null && !organization.getContacts().isEmpty()) {
            ensurePrimaryContact(organization.getContacts());
            for (OrganizationContact contact : organization.getContacts()) {
                OrganizationContactEntity contactEntity = OrganizationContactEntity.fromDto(contact, entity);
                entity.getContacts().add(contactEntity);
            }
        }

        // Add billing contact
        if (organization.getBillingContact() != null) {
            BillingContactEntity billingEntity = BillingContactEntity.fromDto(organization.getBillingContact(), entity);
            entity.setBillingContact(billingEntity);
        }

        // Add contract specialist
        if (organization.getContractSpecialist() != null) {
            ContractSpecialistEntity specialistEntity = ContractSpecialistEntity.fromDto(organization.getContractSpecialist(), entity);
            entity.setContractSpecialist(specialistEntity);
        }

        entity = organizationRepository.save(entity);
        log.info("Created organization: {} ({})", entity.getName(), entity.getAbbreviation());
        return entity.toDto();
    }

    @Transactional
    public Organization update(UUID uuid, Organization organization) throws HopeException {
        OrganizationEntity entity = organizationRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.NOT_FOUND));

        if (organizationRepository.existsByNameIgnoreCaseAndUuidNot(organization.getName(), uuid)) {
            throwError(ResponseCode.ORGANIZATION_NAME_ALREADY_EXISTS);
        }

        entity.setName(organization.getName());
        entity.setAbbreviation(organization.getAbbreviation());
        entity.setReferrerTitle(organization.getReferrerTitle());
        entity.setNote(organization.getNote());

        // Update contacts - clear and re-add
        updateContacts(entity, organization.getContacts());

        // Update billing contact
        updateBillingContact(entity, organization.getBillingContact());

        // Update contract specialist
        updateContractSpecialist(entity, organization.getContractSpecialist());

        entity = organizationRepository.save(entity);
        log.info("Updated organization: {}", entity.getName());
        return entity.toDto();
    }

    @Transactional
    public void updateStatus(UUID uuid, boolean active) throws HopeException {
        OrganizationEntity entity = organizationRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.NOT_FOUND));

        entity.setActive(active);

        // Cascade deactivation to all sub-organizations
        if (!active) {
            List<SubOrganizationEntity> subOrgs = subOrganizationRepository
                    .findByOrganizationIdAndArchiveFalseAndActiveTrue(entity.getId());
            for (SubOrganizationEntity subOrg : subOrgs) {
                subOrg.setActive(false);
                subOrganizationRepository.save(subOrg);
            }
            log.info("Cascaded deactivation to {} sub-organizations under {}", subOrgs.size(), entity.getName());
        }

        organizationRepository.save(entity);
        log.info("Organization {} status set to {}", entity.getName(), active ? "active" : "inactive");
    }

    @Transactional
    public void updateArchiveStatus(UUID uuid, boolean archive) throws HopeException {
        OrganizationEntity entity = organizationRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.NOT_FOUND));

        if (archive) {
            entity.setActive(false);
        }
        entity.setArchive(archive);
        organizationRepository.save(entity);
        log.info("Organization {} archive status set to {}", entity.getName(), archive);
    }

    private void updateContacts(OrganizationEntity entity, List<OrganizationContact> contacts) {
        if (entity.getContacts() == null) {
            entity.setContacts(new ArrayList<>());
        }
        entity.getContacts().clear();

        if (contacts != null && !contacts.isEmpty()) {
            ensurePrimaryContact(contacts);
            for (OrganizationContact contact : contacts) {
                OrganizationContactEntity contactEntity = OrganizationContactEntity.fromDto(contact, entity);
                entity.getContacts().add(contactEntity);
            }
        }
    }

    private void ensurePrimaryContact(List<OrganizationContact> contacts) {
        boolean hasPrimary = contacts.stream().anyMatch(OrganizationContact::isPrimaryContact);
        if (!hasPrimary) {
            contacts.get(0).setPrimaryContact(true);
        }
    }

    private void updateBillingContact(OrganizationEntity entity, BillingContact billingContact) {
        if (billingContact == null) {
            entity.setBillingContact(null);
            return;
        }

        if (entity.getBillingContact() != null) {
            BillingContactEntity existing = entity.getBillingContact();
            existing.setName(billingContact.getName());
            existing.setEmail(billingContact.getEmail());
            existing.setOfficePhone(billingContact.getOfficePhone());
            existing.setAddress(AddressEntity.updateEntity(existing.getAddress(), billingContact.getAddress()));
        } else {
            entity.setBillingContact(BillingContactEntity.fromDto(billingContact, entity));
        }
    }

    private void updateContractSpecialist(OrganizationEntity entity, ContractSpecialist specialist) {
        if (specialist == null) {
            entity.setContractSpecialist(null);
            return;
        }

        if (entity.getContractSpecialist() != null) {
            ContractSpecialistEntity existing = entity.getContractSpecialist();
            existing.setFullName(specialist.getFullName());
            existing.setEmail(specialist.getEmail());
            existing.setPrimaryOfficePhone(specialist.getPrimaryOfficePhone());
            existing.setPhoneType(specialist.getPhoneType());
            existing.setAddress(AddressEntity.updateEntity(existing.getAddress(), specialist.getAddress()));
        } else {
            entity.setContractSpecialist(ContractSpecialistEntity.fromDto(specialist, entity));
        }
    }
}
