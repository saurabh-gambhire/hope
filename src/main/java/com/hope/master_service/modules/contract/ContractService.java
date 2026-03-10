package com.hope.master_service.modules.contract;

import com.hope.master_service.dto.contract.*;
import com.hope.master_service.dto.enums.ContractContactType;
import com.hope.master_service.dto.enums.ContractStatus;
import com.hope.master_service.dto.response.ResponseCode;
import com.hope.master_service.entity.AddressEntity;
import com.hope.master_service.exception.HopeException;
import com.hope.master_service.modules.organization.OrganizationEntity;
import com.hope.master_service.modules.organization.OrganizationRepository;
import com.hope.master_service.modules.organization.SubOrganizationEntity;
import com.hope.master_service.modules.organization.SubOrganizationRepository;
import com.hope.master_service.service.AppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ContractService extends AppService {

    private static final int MAX_TERMS = 8;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SubOrganizationRepository subOrganizationRepository;

    // ======================== SEARCH & GET ========================

    public Page<Contract> search(UUID orgUuid, String search, List<ContractStatus> statuses,
                                 Boolean active, Boolean isTemplate, String createdBy,
                                 Instant createdFrom, Instant createdTo, Pageable pageable) throws HopeException {
        OrganizationEntity org = findOrganization(orgUuid);
        return contractRepository.findAll(
                ContractSpecification.withFilters(org.getId(), search, statuses, active,
                        isTemplate, createdBy, createdFrom, createdTo),
                pageable
        ).map(ContractEntity::toListDto);
    }

    public Contract getByUuid(UUID uuid) throws HopeException {
        return findByUuid(uuid).toDto();
    }

    // ======================== CREATE ========================

    @Transactional
    public Contract create(Contract dto) throws HopeException {
        OrganizationEntity org = findOrganization(dto.getOrganizationUuid());

        // Validate unique identifier
        if (contractRepository.existsByIdentifierIgnoreCase(dto.getIdentifier())) {
            throwError(ResponseCode.CONTRACT_IDENTIFIER_ALREADY_EXISTS);
        }

        // Validate terms
        validateTerms(dto.getTerms());

        ContractEntity entity = ContractEntity.fromDto(dto, org);
        entity = contractRepository.save(entity);

        // Add terms
        addTerms(entity, dto.getTerms());

        // Add sub-org associations
        if (!dto.isAddSubOrgsLater() && dto.getSubOrganizations() != null) {
            addSubOrganizations(entity, dto.getSubOrganizations());
        }

        // Add enrollments
        addEnrollments(entity, dto.getEnrollments());

        // Add attendance statuses
        addAttendanceStatuses(entity, dto.getAttendanceStatuses());

        // Add contacts
        addContacts(entity, dto.getInvoiceSpecialist(), dto.getContractRepresentative());

        // Add offerings
        addOfferings(entity, dto.getOfferings());

        entity = contractRepository.save(entity);
        log.info("Created contract: {} ({})", entity.getName(), entity.getIdentifier());
        return entity.toDto();
    }

    // ======================== UPDATE ========================

    @Transactional
    public Contract update(UUID uuid, Contract dto) throws HopeException {
        ContractEntity entity = findByUuid(uuid);

        // Validate unique identifier (excluding self)
        if (contractRepository.existsByIdentifierIgnoreCaseAndUuidNot(dto.getIdentifier(), uuid)) {
            throwError(ResponseCode.CONTRACT_IDENTIFIER_ALREADY_EXISTS);
        }

        // Validate terms
        validateTerms(dto.getTerms());

        // Update basic fields
        entity.setIdentifier(dto.getIdentifier());
        entity.setName(dto.getName());
        entity.setContractType(dto.getContractType());
        entity.setBudgetAmount(dto.getBudgetAmount());
        if (dto.getPaidAmount() != null) {
            entity.setPaidAmount(dto.getPaidAmount());
        }
        entity.setEnrollmentType(dto.getEnrollmentType());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }

        // Update terms
        updateTerms(entity, dto.getTerms());

        // Update sub-org associations
        updateSubOrganizations(entity, dto.getSubOrganizations());

        // Update enrollments
        updateEnrollments(entity, dto.getEnrollments());

        // Update attendance statuses
        updateAttendanceStatuses(entity, dto.getAttendanceStatuses());

        // Update contacts
        updateContacts(entity, dto.getInvoiceSpecialist(), dto.getContractRepresentative());

        // Update offerings
        updateOfferings(entity, dto.getOfferings());

        entity = contractRepository.save(entity);
        log.info("Updated contract: {} ({})", entity.getName(), entity.getIdentifier());
        return entity.toDto();
    }

    // ======================== STATUS & ARCHIVE ========================

    @Transactional
    public void updateStatus(UUID uuid, ContractStatus newStatus) throws HopeException {
        ContractEntity entity = findByUuid(uuid);
        ContractStatus oldStatus = entity.getStatus();
        entity.setStatus(newStatus);

        if (newStatus == ContractStatus.ACTIVE) {
            entity.setActive(true);
        } else if (newStatus == ContractStatus.SUSPENDED || newStatus == ContractStatus.EXPIRED) {
            entity.setActive(false);
        }

        contractRepository.save(entity);
        log.info("Contract {} status changed from {} to {}", entity.getIdentifier(), oldStatus, newStatus);
    }

    @Transactional
    public void updateArchiveStatus(UUID uuid, boolean archive) throws HopeException {
        ContractEntity entity = findByUuid(uuid);
        if (archive) {
            entity.setActive(false);
        }
        entity.setArchive(archive);
        contractRepository.save(entity);
        log.info("Contract {} archive status set to {}", entity.getIdentifier(), archive);
    }

    // ======================== CLONE ========================

    @Transactional
    public Contract cloneContract(UUID sourceUuid, String newName) throws HopeException {
        ContractEntity source = findByUuid(sourceUuid);

        // Generate unique identifier for clone
        String cloneIdentifier = source.getIdentifier() + "-COPY-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String cloneName = (newName != null && !newName.isBlank()) ? newName : source.getName() + " (Copy)";

        ContractEntity clone = ContractEntity.builder()
                .identifier(cloneIdentifier)
                .name(cloneName)
                .contractType(source.getContractType())
                .budgetAmount(source.getBudgetAmount())
                .paidAmount(java.math.BigDecimal.ZERO)
                .enrollmentType(source.getEnrollmentType())
                .status(ContractStatus.DRAFT)
                .isTemplate(false)
                .sourceContract(source)
                .organization(source.getOrganization())
                .terms(new ArrayList<>())
                .subOrganizations(new ArrayList<>())
                .enrollments(new ArrayList<>())
                .attendanceStatuses(new ArrayList<>())
                .contacts(new ArrayList<>())
                .offerings(new ArrayList<>())
                .build();

        clone = contractRepository.save(clone);

        // Clone terms
        if (source.getTerms() != null) {
            for (ContractTermEntity term : source.getTerms()) {
                clone.getTerms().add(ContractTermEntity.fromDto(term.toDto(), clone));
            }
        }

        // Clone enrollments
        if (source.getEnrollments() != null) {
            for (ContractEnrollmentEntity enrollment : source.getEnrollments()) {
                clone.getEnrollments().add(ContractEnrollmentEntity.builder()
                        .contract(clone).enrollmentName(enrollment.getEnrollmentName()).build());
            }
        }

        // Clone attendance statuses
        if (source.getAttendanceStatuses() != null) {
            for (ContractAttendanceStatusEntity status : source.getAttendanceStatuses()) {
                clone.getAttendanceStatuses().add(ContractAttendanceStatusEntity.builder()
                        .contract(clone).statusName(status.getStatusName()).build());
            }
        }

        // Clone contacts
        if (source.getContacts() != null) {
            for (ContractContactEntity contact : source.getContacts()) {
                ContractContactEntity clonedContact = ContractContactEntity.builder()
                        .contract(clone)
                        .contactType(contact.getContactType())
                        .name(contact.getName())
                        .email(contact.getEmail())
                        .officePhone(contact.getOfficePhone())
                        .build();
                if (contact.getAddress() != null) {
                    clonedContact.setAddress(AddressEntity.toEntity(AddressEntity.toDto(contact.getAddress())));
                }
                clone.getContacts().add(clonedContact);
            }
        }

        // Clone offerings
        if (source.getOfferings() != null) {
            for (ContractOfferingEntity offering : source.getOfferings()) {
                clone.getOfferings().add(ContractOfferingEntity.fromDto(offering.toDto(), clone));
            }
        }

        clone = contractRepository.save(clone);
        log.info("Cloned contract {} from source {}", clone.getIdentifier(), source.getIdentifier());
        return clone.toDto();
    }

    // ======================== SUB-ORG ASSIGNMENT ========================

    @Transactional
    public Contract assignSubOrganization(UUID contractUuid, UUID subOrgUuid) throws HopeException {
        ContractEntity contract = findByUuid(contractUuid);
        SubOrganizationEntity subOrg = subOrganizationRepository.findByUuid(subOrgUuid)
                .orElseThrow(() -> throwException(ResponseCode.SUB_ORGANIZATION_NOT_FOUND));

        if (contractRepository.existsByIdAndSubOrganizations_SubOrganization_Id(contract.getId(), subOrg.getId())) {
            throwError(ResponseCode.CONTRACT_SUB_ORG_ALREADY_ASSIGNED);
        }

        contract.getSubOrganizations().add(ContractSubOrganizationEntity.builder()
                .contract(contract).subOrganization(subOrg).build());

        contract = contractRepository.save(contract);
        log.info("Assigned sub-org {} to contract {}", subOrg.getName(), contract.getIdentifier());
        return contract.toDto();
    }

    @Transactional
    public void unassignSubOrganization(UUID contractUuid, UUID subOrgUuid) throws HopeException {
        ContractEntity contract = findByUuid(contractUuid);
        contract.getSubOrganizations().removeIf(
                cso -> cso.getSubOrganization().getUuid().equals(subOrgUuid));
        contractRepository.save(contract);
        log.info("Unassigned sub-org {} from contract {}", subOrgUuid, contract.getIdentifier());
    }

    // ======================== CONTRACTS BY SUB-ORG ========================

    public List<Contract> getBySubOrganization(UUID subOrgUuid) throws HopeException {
        SubOrganizationEntity subOrg = subOrganizationRepository.findByUuid(subOrgUuid)
                .orElseThrow(() -> throwException(ResponseCode.SUB_ORGANIZATION_NOT_FOUND));

        return contractRepository.findAll().stream()
                .filter(c -> !c.isArchive() && c.hasSubOrganization(subOrg.getId()))
                .map(ContractEntity::toListDto)
                .toList();
    }

    // ======================== PRIVATE HELPERS ========================

    private ContractEntity findByUuid(UUID uuid) throws HopeException {
        return contractRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.CONTRACT_NOT_FOUND));
    }

    private OrganizationEntity findOrganization(UUID orgUuid) throws HopeException {
        return organizationRepository.findByUuid(orgUuid)
                .orElseThrow(() -> throwException(ResponseCode.ORGANIZATION_NOT_FOUND));
    }

    private void validateTerms(List<ContractTerm> terms) throws HopeException {
        if (terms == null || terms.isEmpty()) return;

        if (terms.size() > MAX_TERMS) {
            throwError(ResponseCode.CONTRACT_MAX_TERMS_EXCEEDED, String.valueOf(MAX_TERMS));
        }

        for (ContractTerm term : terms) {
            if (term.getStartDate() != null && term.getEndDate() != null) {
                if (!term.getEndDate().isAfter(term.getStartDate())) {
                    throwError(ResponseCode.CONTRACT_TERM_INVALID_DATES);
                }
            }
        }

        // Check for overlaps
        for (int i = 0; i < terms.size(); i++) {
            for (int j = i + 1; j < terms.size(); j++) {
                if (datesOverlap(terms.get(i).getStartDate(), terms.get(i).getEndDate(),
                        terms.get(j).getStartDate(), terms.get(j).getEndDate())) {
                    throwError(ResponseCode.CONTRACT_TERM_OVERLAP);
                }
            }
        }
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) return false;
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }

    private void addTerms(ContractEntity entity, List<ContractTerm> terms) {
        if (terms == null) return;
        for (ContractTerm term : terms) {
            entity.getTerms().add(ContractTermEntity.fromDto(term, entity));
        }
    }

    private void updateTerms(ContractEntity entity, List<ContractTerm> terms) {
        if (entity.getTerms() == null) {
            entity.setTerms(new ArrayList<>());
        }
        entity.getTerms().clear();
        addTerms(entity, terms);
    }

    private void addSubOrganizations(ContractEntity entity, List<ContractSubOrg> subOrgs) throws HopeException {
        if (subOrgs == null) return;
        for (ContractSubOrg so : subOrgs) {
            SubOrganizationEntity subOrg = subOrganizationRepository.findByUuid(so.getSubOrganizationUuid())
                    .orElseThrow(() -> throwException(ResponseCode.SUB_ORGANIZATION_NOT_FOUND));
            entity.getSubOrganizations().add(ContractSubOrganizationEntity.builder()
                    .contract(entity).subOrganization(subOrg).build());
        }
    }

    private void updateSubOrganizations(ContractEntity entity, List<ContractSubOrg> subOrgs) throws HopeException {
        if (entity.getSubOrganizations() == null) {
            entity.setSubOrganizations(new ArrayList<>());
        }
        entity.getSubOrganizations().clear();
        addSubOrganizations(entity, subOrgs);
    }

    private void addEnrollments(ContractEntity entity, List<String> enrollments) {
        if (enrollments == null) return;
        for (String name : enrollments) {
            entity.getEnrollments().add(ContractEnrollmentEntity.builder()
                    .contract(entity).enrollmentName(name).build());
        }
    }

    private void updateEnrollments(ContractEntity entity, List<String> enrollments) {
        if (entity.getEnrollments() == null) {
            entity.setEnrollments(new ArrayList<>());
        }
        entity.getEnrollments().clear();
        addEnrollments(entity, enrollments);
    }

    private void addAttendanceStatuses(ContractEntity entity, List<String> statuses) {
        if (statuses == null) return;
        for (String name : statuses) {
            entity.getAttendanceStatuses().add(ContractAttendanceStatusEntity.builder()
                    .contract(entity).statusName(name).build());
        }
    }

    private void updateAttendanceStatuses(ContractEntity entity, List<String> statuses) {
        if (entity.getAttendanceStatuses() == null) {
            entity.setAttendanceStatuses(new ArrayList<>());
        }
        entity.getAttendanceStatuses().clear();
        addAttendanceStatuses(entity, statuses);
    }

    private void addContacts(ContractEntity entity, ContractContact invoiceSpecialist,
                             ContractContact representative) {
        if (invoiceSpecialist != null) {
            invoiceSpecialist.setContactType(ContractContactType.INVOICE_SPECIALIST);
            entity.getContacts().add(ContractContactEntity.fromDto(invoiceSpecialist, entity));
        }
        if (representative != null) {
            representative.setContactType(ContractContactType.REPRESENTATIVE);
            entity.getContacts().add(ContractContactEntity.fromDto(representative, entity));
        }
    }

    private void updateContacts(ContractEntity entity, ContractContact invoiceSpecialist,
                                ContractContact representative) {
        if (entity.getContacts() == null) {
            entity.setContacts(new ArrayList<>());
        }
        entity.getContacts().clear();
        addContacts(entity, invoiceSpecialist, representative);
    }

    private void addOfferings(ContractEntity entity, List<ContractOffering> offerings) {
        if (offerings == null) return;
        for (ContractOffering offering : offerings) {
            entity.getOfferings().add(ContractOfferingEntity.fromDto(offering, entity));
        }
    }

    private void updateOfferings(ContractEntity entity, List<ContractOffering> offerings) {
        if (entity.getOfferings() == null) {
            entity.setOfferings(new ArrayList<>());
        }
        entity.getOfferings().clear();
        addOfferings(entity, offerings);
    }
}
