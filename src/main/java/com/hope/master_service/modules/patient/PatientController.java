package com.hope.master_service.modules.patient;

import com.hope.master_service.controller.AppController;
import com.hope.master_service.dto.patient.Patient;
import com.hope.master_service.dto.response.Response;
import com.hope.master_service.dto.response.ResponseCode;
import com.hope.master_service.exception.HopeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/master/patients")
@RequiredArgsConstructor
public class PatientController extends AppController {

    private final PatientService patientService;

    @GetMapping
    public ResponseEntity<Response> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        Page<Patient> patients = patientService.getAll(
                PageRequest.of(page, size, Sort.Direction.fromString(sortDirection), sortBy));
        return data(patients);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Response> getByUuid(@PathVariable UUID uuid) throws HopeException {
        return data(patientService.getByUuid(uuid));
    }

    @PostMapping
    public ResponseEntity<Response> create(@Valid @RequestBody Patient patient) throws HopeException {
        Patient created = patientService.create(patient);
        return data(ResponseCode.PATIENT_CREATED,
                messageService.getMessage(ResponseCode.PATIENT_CREATED), created);
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<Response> update(@PathVariable UUID uuid,
                                           @Valid @RequestBody Patient patient) throws HopeException {
        Patient updated = patientService.update(uuid, patient);
        return data(ResponseCode.PATIENT_UPDATE,
                messageService.getMessage(ResponseCode.PATIENT_UPDATE), updated);
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<Response> updateStatus(@PathVariable UUID uuid,
                                                 @RequestParam boolean active) throws HopeException {
        patientService.updateStatus(uuid, active);
        return success(active ? ResponseCode.PATIENT_ENABLED : ResponseCode.PATIENT_DISABLED);
    }

    @PatchMapping("/{uuid}/archive")
    public ResponseEntity<Response> updateArchiveStatus(@PathVariable UUID uuid,
                                                        @RequestParam boolean archive) throws HopeException {
        patientService.updateArchiveStatus(uuid, archive);
        return success(archive ? ResponseCode.PATIENT_ARCHIVED : ResponseCode.PATIENT_UNARCHIVED);
    }
}
