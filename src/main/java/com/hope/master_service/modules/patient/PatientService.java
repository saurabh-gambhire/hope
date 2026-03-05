package com.hope.master_service.modules.patient;

import com.hope.master_service.dto.enums.Roles;
import com.hope.master_service.dto.patient.Patient;
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
public class PatientService extends AppService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserService userService;

    public Page<Patient> getAll(Pageable pageable) {
        return patientRepository.findByUserArchiveFalse(pageable)
                .map(PatientEntity::toDto);
    }

    public Patient getByUuid(UUID uuid) throws HopeException {
        PatientEntity entity = patientRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.PATIENT_NOT_FOUND));
        return entity.toDto();
    }

    @Transactional
    public Patient create(Patient patient) throws HopeException {
        User user = User.builder()
                .email(patient.getEmail())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .middleName(patient.getMiddleName())
                .phone(patient.getPhone())
                .role(Roles.PATIENT)
                .birthDate(patient.getBirthDate())
                .build();

        UserEntity userEntity = userService.createUserEntity(user);

        PatientEntity entity = PatientEntity.fromDto(patient, userEntity);
        entity = patientRepository.save(entity);

        log.info("Created patient: {} (MRN: {})", userEntity.getEmail(), entity.getMrn());
        return entity.toDto();
    }

    @Transactional
    public Patient update(UUID uuid, Patient patient) throws HopeException {
        PatientEntity entity = patientRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.PATIENT_NOT_FOUND));

        UserEntity userEntity = entity.getUser();
        userEntity.setFirstName(patient.getFirstName());
        userEntity.setLastName(patient.getLastName());
        userEntity.setMiddleName(patient.getMiddleName());
        userEntity.setPhone(patient.getPhone());
        userEntity.setBirthDate(patient.getBirthDate());

        entity.setGender(patient.getGender());
        entity.setSsn(patient.getSsn());
        entity.setTimezone(patient.getTimezone());
        entity.setRegistrationDate(patient.getRegistrationDate());
        entity.setFaxNumber(patient.getFaxNumber());
        entity.setMobileNumber(patient.getMobileNumber());
        entity.setHomePhone(patient.getHomePhone());
        entity.setEmailConsent(patient.isEmailConsent());
        entity.setMessageConsent(patient.isMessageConsent());
        entity.setCallConsent(patient.isCallConsent());

        entity = patientRepository.save(entity);
        return entity.toDto();
    }

    @Transactional
    public void updateStatus(UUID uuid, boolean active) throws HopeException {
        PatientEntity entity = patientRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.PATIENT_NOT_FOUND));
        userService.updateStatus(entity.getUser().getUuid(), active);
    }

    @Transactional
    public void updateArchiveStatus(UUID uuid, boolean archive) throws HopeException {
        PatientEntity entity = patientRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.PATIENT_NOT_FOUND));
        userService.updateArchiveStatus(entity.getUser().getUuid(), archive);
    }
}
