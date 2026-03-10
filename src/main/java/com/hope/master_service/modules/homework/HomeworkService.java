package com.hope.master_service.modules.homework;

import com.hope.master_service.dto.homework.Homework;
import com.hope.master_service.dto.homework.HomeworkCurriculum;
import com.hope.master_service.dto.homework.HomeworkDocumentVersion;
import com.hope.master_service.dto.response.ResponseCode;
import com.hope.master_service.exception.HopeException;
import com.hope.master_service.modules.contract.ContractEntity;
import com.hope.master_service.modules.contract.ContractRepository;
import com.hope.master_service.modules.organization.SubOrganizationEntity;
import com.hope.master_service.modules.organization.SubOrganizationRepository;
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
import java.util.stream.Collectors;

@Service
@Slf4j
public class HomeworkService extends AppService {

    @Autowired
    private HomeworkRepository homeworkRepository;

    @Autowired
    private HomeworkDocumentVersionRepository documentVersionRepository;

    @Autowired
    private HomeworkCurriculumRepository curriculumRepository;

    @Autowired
    private SubOrganizationRepository subOrganizationRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private AwsService awsService;

    // ======================== HOMEWORK LIBRARY CRUD ========================

    public Page<Homework> search(String search, UUID subOrganizationUuid, UUID contractUuid,
                                 Boolean active, Pageable pageable) {
        return homeworkRepository.findAll(
                HomeworkSpecification.withFilters(search, subOrganizationUuid, contractUuid, active),
                pageable
        ).map(HomeworkEntity::toListDto);
    }

    public Homework getByUuid(UUID uuid) throws HopeException {
        HomeworkEntity entity = findByUuid(uuid);
        Homework dto = entity.toDto();
        resolveDocumentUrl(dto, entity);
        return dto;
    }

    @Transactional
    public Homework create(Homework dto) throws HopeException {
        SubOrganizationEntity subOrg = subOrganizationRepository.findByUuid(dto.getSubOrganizationUuid())
                .orElseThrow(() -> throwException(ResponseCode.SUB_ORGANIZATION_NOT_FOUND));

        ContractEntity contract = contractRepository.findByUuid(dto.getContractUuid())
                .orElseThrow(() -> throwException(ResponseCode.CONTRACT_NOT_FOUND));

        // Validate contract belongs to sub-org
        if (!contract.hasSubOrganization(subOrg.getId())) {
            throwError(ResponseCode.CONTRACT_NOT_FOUND, "Contract does not belong to the selected sub-organization");
        }

        // Duplicate check: same name under same sub-org + contract
        if (homeworkRepository.existsByNameIgnoreCaseAndSubOrganizationIdAndContractId(
                dto.getName(), subOrg.getId(), contract.getId())) {
            throwError(ResponseCode.HOMEWORK_NAME_ALREADY_EXISTS);
        }

        HomeworkEntity entity = HomeworkEntity.fromDto(dto, subOrg, contract);
        entity = homeworkRepository.save(entity);

        // Handle document upload
        if (StringUtils.isNotBlank(dto.getDocument())) {
            uploadDocument(entity, dto.getDocument(), dto.getUploadFileName(), dto.getUploadFileSize());
            entity = homeworkRepository.save(entity);
        }

        log.info("Created homework: {} under sub-org: {} / contract: {}", entity.getName(),
                subOrg.getName(), contract.getName());
        return entity.toDto();
    }

    @Transactional
    public Homework update(UUID uuid, Homework dto) throws HopeException {
        HomeworkEntity entity = findByUuid(uuid);

        SubOrganizationEntity subOrg = subOrganizationRepository.findByUuid(dto.getSubOrganizationUuid())
                .orElseThrow(() -> throwException(ResponseCode.SUB_ORGANIZATION_NOT_FOUND));

        ContractEntity contract = contractRepository.findByUuid(dto.getContractUuid())
                .orElseThrow(() -> throwException(ResponseCode.CONTRACT_NOT_FOUND));

        if (!contract.hasSubOrganization(subOrg.getId())) {
            throwError(ResponseCode.CONTRACT_NOT_FOUND, "Contract does not belong to the selected sub-organization");
        }

        // Duplicate check excluding current
        if (homeworkRepository.existsByNameIgnoreCaseAndSubOrganizationIdAndContractIdAndUuidNot(
                dto.getName(), subOrg.getId(), contract.getId(), uuid)) {
            throwError(ResponseCode.HOMEWORK_NAME_ALREADY_EXISTS);
        }

        entity.setName(dto.getName());
        entity.setContent(dto.getContent());
        entity.setSubOrganization(subOrg);
        entity.setContract(contract);

        // Handle document upload/replacement
        if (StringUtils.isNotBlank(dto.getDocument())) {
            uploadDocument(entity, dto.getDocument(), dto.getUploadFileName(), dto.getUploadFileSize());
        }

        entity = homeworkRepository.save(entity);
        log.info("Updated homework: {}", entity.getName());

        Homework result = entity.toDto();
        resolveDocumentUrl(result, entity);
        return result;
    }

    @Transactional
    public void updateArchiveStatus(UUID uuid, boolean archive) throws HopeException {
        HomeworkEntity entity = findByUuid(uuid);
        if (archive) {
            entity.setActive(false);
        }
        entity.setArchive(archive);
        homeworkRepository.save(entity);
        log.info("Homework {} archive status set to {}", entity.getName(), archive);
    }

    @Transactional
    public void delete(UUID uuid) throws HopeException {
        HomeworkEntity entity = findByUuid(uuid);

        // Check if homework is in any curriculum
        long curriculumCount = curriculumRepository.countByHomeworkId(entity.getId());
        if (curriculumCount > 0) {
            throwError(ResponseCode.HOMEWORK_IN_USE,
                    String.valueOf(curriculumCount));
        }

        // Delete S3 document and all versions
        deleteAllDocumentVersions(entity);
        if (StringUtils.isNotBlank(entity.getDocumentS3Key())) {
            awsService.deleteObject(entity.getDocumentS3Key());
        }

        homeworkRepository.delete(entity);
        log.info("Deleted homework: {}", entity.getName());
    }

    // ======================== DOCUMENT MANAGEMENT ========================

    @Transactional
    public Homework replaceDocument(UUID uuid, String base64Document, String fileName, Long fileSize) throws HopeException {
        HomeworkEntity entity = findByUuid(uuid);
        uploadDocument(entity, base64Document, fileName, fileSize);
        entity = homeworkRepository.save(entity);
        log.info("Replaced document for homework: {} (now version {})", entity.getName(), entity.getCurrentVersion());

        Homework result = entity.toDto();
        resolveDocumentUrl(result, entity);
        return result;
    }

    @Transactional
    public Homework removeDocument(UUID uuid) throws HopeException {
        HomeworkEntity entity = findByUuid(uuid);

        if (StringUtils.isNotBlank(entity.getDocumentS3Key())) {
            awsService.deleteObject(entity.getDocumentS3Key());
        }

        entity.setDocumentFileName(null);
        entity.setDocumentFileSize(null);
        entity.setDocumentS3Key(null);
        // Keep version history intact

        entity = homeworkRepository.save(entity);
        log.info("Removed document from homework: {}", entity.getName());
        return entity.toDto();
    }

    public List<HomeworkDocumentVersion> getDocumentVersions(UUID uuid) throws HopeException {
        HomeworkEntity entity = findByUuid(uuid);
        return documentVersionRepository.findByHomeworkIdOrderByVersionDesc(entity.getId())
                .stream()
                .map(v -> {
                    HomeworkDocumentVersion dto = v.toDto();
                    try {
                        dto.setDocumentUrl(awsService.getPreSignedUrl(v.getS3Key()));
                    } catch (HopeException e) {
                        log.warn("Failed to generate pre-signed URL for version {}", v.getVersion());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public String getDocumentUrl(UUID uuid) throws HopeException {
        HomeworkEntity entity = findByUuid(uuid);
        if (StringUtils.isBlank(entity.getDocumentS3Key())) {
            throwError(ResponseCode.HOMEWORK_DOCUMENT_NOT_FOUND);
        }
        return awsService.getPreSignedUrl(entity.getDocumentS3Key());
    }

    // ======================== CURRICULUM MANAGEMENT ========================

    public List<HomeworkCurriculum> getCurriculum(UUID subOrgUuid, UUID contractUuid) throws HopeException {
        SubOrganizationEntity subOrg = subOrganizationRepository.findByUuid(subOrgUuid)
                .orElseThrow(() -> throwException(ResponseCode.SUB_ORGANIZATION_NOT_FOUND));

        ContractEntity contract = contractRepository.findByUuid(contractUuid)
                .orElseThrow(() -> throwException(ResponseCode.CONTRACT_NOT_FOUND));

        return curriculumRepository
                .findBySubOrganizationIdAndContractIdOrderBySequenceOrder(subOrg.getId(), contract.getId())
                .stream()
                .map(HomeworkCurriculumEntity::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public HomeworkCurriculum addToCurriculum(HomeworkCurriculum dto) throws HopeException {
        SubOrganizationEntity subOrg = subOrganizationRepository.findByUuid(dto.getSubOrganizationUuid())
                .orElseThrow(() -> throwException(ResponseCode.SUB_ORGANIZATION_NOT_FOUND));

        ContractEntity contract = contractRepository.findByUuid(dto.getContractUuid())
                .orElseThrow(() -> throwException(ResponseCode.CONTRACT_NOT_FOUND));

        HomeworkEntity homework = findByUuid(dto.getHomeworkUuid());

        // Check if already mapped
        if (curriculumRepository.existsBySubOrganizationIdAndContractIdAndHomeworkId(
                subOrg.getId(), contract.getId(), homework.getId())) {
            throwError(ResponseCode.HOMEWORK_ALREADY_IN_CURRICULUM);
        }

        HomeworkCurriculumEntity entity = HomeworkCurriculumEntity.builder()
                .subOrganization(subOrg)
                .contract(contract)
                .homework(homework)
                .sequenceOrder(dto.getSequenceOrder())
                .build();

        entity = curriculumRepository.save(entity);
        log.info("Added homework '{}' to curriculum for sub-org: {} / contract: {}",
                homework.getName(), subOrg.getName(), contract.getName());
        return entity.toDto();
    }

    @Transactional
    public void removeFromCurriculum(UUID curriculumUuid) throws HopeException {
        HomeworkCurriculumEntity entity = curriculumRepository.findByUuid(curriculumUuid)
                .orElseThrow(() -> throwException(ResponseCode.HOMEWORK_CURRICULUM_NOT_FOUND));

        curriculumRepository.delete(entity);
        log.info("Removed homework '{}' from curriculum for sub-org: {} / contract: {}",
                entity.getHomework().getName(),
                entity.getSubOrganization().getName(),
                entity.getContract().getName());
    }

    @Transactional
    public void updateCurriculumOrder(UUID subOrgUuid, UUID contractUuid, List<HomeworkCurriculum> orderedItems) throws HopeException {
        SubOrganizationEntity subOrg = subOrganizationRepository.findByUuid(subOrgUuid)
                .orElseThrow(() -> throwException(ResponseCode.SUB_ORGANIZATION_NOT_FOUND));

        ContractEntity contract = contractRepository.findByUuid(contractUuid)
                .orElseThrow(() -> throwException(ResponseCode.CONTRACT_NOT_FOUND));

        List<HomeworkCurriculumEntity> existing = curriculumRepository
                .findBySubOrganizationIdAndContractIdOrderBySequenceOrder(subOrg.getId(), contract.getId());

        for (HomeworkCurriculum item : orderedItems) {
            existing.stream()
                    .filter(e -> e.getUuid().equals(item.getUuid()))
                    .findFirst()
                    .ifPresent(e -> e.setSequenceOrder(item.getSequenceOrder()));
        }

        curriculumRepository.saveAll(existing);
        log.info("Updated curriculum order for sub-org: {} / contract: {}", subOrg.getName(), contract.getName());
    }

    // ======================== CLIENT CURRICULUM (filtered for clinicians) ========================

    public List<Homework> getClientCurriculum(UUID subOrgUuid, UUID contractUuid) throws HopeException {
        SubOrganizationEntity subOrg = subOrganizationRepository.findByUuid(subOrgUuid)
                .orElseThrow(() -> throwException(ResponseCode.SUB_ORGANIZATION_NOT_FOUND));

        ContractEntity contract = contractRepository.findByUuid(contractUuid)
                .orElseThrow(() -> throwException(ResponseCode.CONTRACT_NOT_FOUND));

        return curriculumRepository
                .findBySubOrganizationIdAndContractIdOrderBySequenceOrder(subOrg.getId(), contract.getId())
                .stream()
                .map(c -> {
                    Homework dto = c.getHomework().toListDto();
                    try {
                        if (StringUtils.isNotBlank(c.getHomework().getDocumentS3Key())) {
                            dto.setDocumentUrl(awsService.getPreSignedUrl(c.getHomework().getDocumentS3Key()));
                        }
                    } catch (HopeException e) {
                        log.warn("Failed to resolve document URL for homework: {}", c.getHomework().getName());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // ======================== PRIVATE HELPERS ========================

    private HomeworkEntity findByUuid(UUID uuid) throws HopeException {
        return homeworkRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.HOMEWORK_NOT_FOUND));
    }

    private void uploadDocument(HomeworkEntity entity, String base64Document, String fileName, Long fileSize) throws HopeException {
        // Validate file type (PDF, JPEG, PNG)
        if (fileName != null) {
            String lowerName = fileName.toLowerCase();
            if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".jpeg")
                    && !lowerName.endsWith(".jpg") && !lowerName.endsWith(".png")) {
                throwError(ResponseCode.HOMEWORK_INVALID_FILE_TYPE);
            }
        }

        // Upload to S3
        String s3Key = awsService.uploadBase64(base64Document, "homework", entity.getUuid());
        if (s3Key == null) {
            throwError(ResponseCode.AWS_ERROR, "Failed to upload homework document");
        }

        // Increment version
        int newVersion = entity.getCurrentVersion() + 1;

        // Create version history entry
        HomeworkDocumentVersionEntity versionEntity = HomeworkDocumentVersionEntity.builder()
                .homework(entity)
                .version(newVersion)
                .fileName(fileName != null ? fileName : "document.pdf")
                .fileSize(fileSize != null ? fileSize : 0)
                .s3Key(s3Key)
                .uploadedBy(getCurrentUserEmail())
                .uploadedAt(Instant.now())
                .build();

        entity.getDocumentVersions().add(versionEntity);

        // Update current document info
        entity.setDocumentFileName(fileName);
        entity.setDocumentFileSize(fileSize);
        entity.setDocumentS3Key(s3Key);
        entity.setCurrentVersion(newVersion);
    }

    private void deleteAllDocumentVersions(HomeworkEntity entity) {
        if (entity.getDocumentVersions() != null) {
            for (HomeworkDocumentVersionEntity version : entity.getDocumentVersions()) {
                try {
                    awsService.deleteObject(version.getS3Key());
                } catch (HopeException e) {
                    log.warn("Failed to delete S3 object for version {}: {}", version.getVersion(), e.getMessage());
                }
            }
        }
    }

    private void resolveDocumentUrl(Homework dto, HomeworkEntity entity) {
        if (StringUtils.isNotBlank(entity.getDocumentS3Key())) {
            try {
                dto.setDocumentUrl(awsService.getPreSignedUrl(entity.getDocumentS3Key()));
            } catch (HopeException e) {
                log.warn("Failed to resolve document URL for homework: {}", entity.getName());
            }
        }
    }

    private String getCurrentUserEmail() {
        try {
            return getCurrentUser().getEmail();
        } catch (HopeException e) {
            return null;
        }
    }
}
